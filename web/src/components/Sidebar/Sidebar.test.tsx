// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { AxiosRequestConfig } from 'axios';
import Sidebar from './index';
import { SidebarProps } from './sidebarTypes';
import { fontAwesomeIcon } from '../../test/fontAwesomeIcon';
import { clazzFixture, moduleFixture } from '../../test/fixtures/moduleFixtures';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';

type SidebarCallbacks = Omit<SidebarProps, 'isOpen' | 'modules'>;

const orders = moduleFixture({
  id: 'm1',
  name: 'orders',
  allDependencies: [
    { packageName: 'java.util', types: [] },
    { packageName: 'org.springframework.stereotype', types: [] },
  ],
});
const billing = moduleFixture({ id: 'm2', name: 'billing' });

let fakeApi: FakeAxiosAdapter;
const realAdapter: AxiosRequestConfig['adapter'] = axios.defaults.adapter;

beforeEach(() => {
  fakeApi = new FakeAxiosAdapter();
  axios.defaults.adapter = fakeApi.adapter;
});

afterEach(() => {
  axios.defaults.adapter = realAdapter;
});

function renderSidebar(callbacks: Partial<SidebarCallbacks> = {}) {
  return render(
    <Sidebar
      isOpen
      modules={[orders, billing]}
      onDeleteRefresh={() => {}}
      onRenameRefresh={() => {}}
      onSplitRefresh={() => {}}
      onRefClazzModeRefresh={() => {}}
      {...callbacks}
    />
  );
}

function sentBody(method: string, url: string): unknown {
  return JSON.parse(String(fakeApi.requestsTo(method, url)[0].body));
}

describe('Sidebar module list', () => {
  it('lists every module as a card', () => {
    renderSidebar();

    expect(screen.getByText('orders')).toBeInTheDocument();
    expect(screen.getByText('billing')).toBeInTheDocument();
  });

  it('asks for confirmation before deleting a module from its card menu', async () => {
    const { container } = renderSidebar();

    await userEvent.click(fontAwesomeIcon(container, 'ellipsis-vertical', 1));
    expect(screen.getByText('Rename')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Delete'));

    expect(screen.getByText('Delete Module')).toBeInTheDocument();
  });

  it('renames a module when Enter confirms the new name', async () => {
    fakeApi.respondTo('POST', '/api/module/rename', null);
    const onRenameRefresh = vi.fn();
    const { container } = renderSidebar({ onRenameRefresh });

    await userEvent.click(fontAwesomeIcon(container, 'ellipsis-vertical', 0));
    await userEvent.click(screen.getByText('Rename'));
    await userEvent.clear(screen.getByDisplayValue('orders'));
    await userEvent.type(screen.getByRole('textbox'), 'checkout{Enter}');

    await vi.waitFor(() => expect(onRenameRefresh).toHaveBeenCalledWith('m1', 'checkout'));
    expect(fakeApi.requestsTo('POST', '/api/module/rename')[0].params).toEqual({
      moduleId: 'm1',
      newName: 'checkout',
    });
  });

  it('restores the name and calls nothing when Escape cancels a rename', async () => {
    const { container } = renderSidebar();

    await userEvent.click(fontAwesomeIcon(container, 'ellipsis-vertical', 0));
    await userEvent.click(screen.getByText('Rename'));
    await userEvent.type(screen.getByRole('textbox'), '-draft{Escape}');

    expect(screen.getByText('orders')).toBeInTheDocument();
    expect(fakeApi.requests).toHaveLength(0);
  });
});

describe('Sidebar module detail', () => {
  it('opens a module and offers split and ref-class actions on right click', async () => {
    renderSidebar();

    await userEvent.click(screen.getByText('orders'));
    fireEvent.contextMenu(screen.getByText('OrderRepository'));

    expect(screen.getByRole('heading', { name: 'orders' })).toBeInTheDocument();
    expect(screen.getByText('Set as Ref Classes')).toBeInTheDocument();
    expect(screen.getByText('Split')).toBeInTheDocument();
  });

  it('lists the module dependencies by package when the section is expanded', async () => {
    // Regression: the section read `module.types`, which the API never sends, and crashed.
    renderSidebar();

    await userEvent.click(screen.getByText('orders'));
    await userEvent.click(screen.getByText('Dependencies'));

    expect(screen.getByText('java.util')).toBeInTheDocument();
    expect(screen.getByText('org.springframework.stereotype')).toBeInTheDocument();
  });

  it('says so when a module has no dependencies', async () => {
    render(
      <Sidebar
        isOpen
        modules={[moduleFixture({ id: 'm3', name: 'shipping', allDependencies: [] })]}
        onDeleteRefresh={() => {}}
        onRenameRefresh={() => {}}
        onSplitRefresh={() => {}}
        onRefClazzModeRefresh={() => {}}
      />
    );

    await userEvent.click(screen.getByText('shipping'));
    await userEvent.click(screen.getByText('Dependencies'));

    expect(screen.getByRole('heading', { name: 'shipping' })).toBeInTheDocument();
    expect(screen.getByText('No dependencies')).toBeInTheDocument();
  });

  it('switches a reference class from ALLOW to MUST', async () => {
    const mustRefClazz = clazzFixture({ enforceMode: 'MUST' });
    fakeApi.respondTo('POST', '/api/module/ref-clazz-mode', {
      refClazzes: [mustRefClazz],
      clazzes: orders.clazzes,
    });
    const onRefClazzModeRefresh = vi.fn();
    renderSidebar({ onRefClazzModeRefresh });

    await userEvent.click(screen.getByText('orders'));
    await userEvent.click(screen.getByRole('button', { name: 'ALLOW' }));

    expect(await screen.findByRole('button', { name: 'MUST' })).toBeInTheDocument();
    expect(sentBody('POST', '/api/module/ref-clazz-mode')).toEqual({
      moduleId: 'm1',
      classId: 'c1',
      mode: 'MUST',
    });
    expect(onRefClazzModeRefresh).toHaveBeenCalledWith(
      'm1',
      expect.objectContaining({ refClazzes: [mustRefClazz] })
    );
  });
});

describe('Sidebar split', () => {
  it('hands the split modules on exactly as the API returns them', async () => {
    // Regression: the modules used to be rebuilt by hand without refClazzes, and the graph
    // node for a split module then crashed reading its reference class.
    const halves = [
      moduleFixture({ id: 'a', name: 'orders-a' }),
      moduleFixture({ id: 'b', name: 'orders-b' }),
    ];
    fakeApi.respondTo('POST', '/api/module/split', { newModules: halves });
    const onSplitRefresh = vi.fn();
    renderSidebar({ onSplitRefresh });

    await userEvent.click(screen.getByText('orders'));
    fireEvent.contextMenu(screen.getByText('OrderRepository'));
    await userEvent.click(screen.getByText('Split'));

    await vi.waitFor(() => expect(onSplitRefresh).toHaveBeenCalledWith('m1', halves));
    expect(sentBody('POST', '/api/module/split')).toEqual({ moduleId: 'm1', classIds: ['c2'] });
  });
});

// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { AxiosRequestConfig } from 'axios';
import Sidebar from './index';
import { fontAwesomeIcon } from '../../test/fontAwesomeIcon';
import { moduleFixture } from '../../test/fixtures/moduleFixtures';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { ModuleData } from '../../interface/ModuleData';

function renderSidebar(
  onSplitRefresh: (oldId: string, newModules: ModuleData[]) => void = () => {}
) {
  return render(
    <Sidebar
      isOpen
      modules={[
        moduleFixture({ id: 'm1', name: 'orders' }),
        moduleFixture({ id: 'm2', name: 'billing' }),
      ]}
      onDeleteRefresh={() => {}}
      onRenameRefresh={() => {}}
      onSplitRefresh={onSplitRefresh}
      onRefClazzModeRefresh={() => {}}
    />
  );
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
});

describe('Sidebar split', () => {
  const realAdapter: AxiosRequestConfig['adapter'] = axios.defaults.adapter;

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  it('hands the split modules on exactly as the API returns them', async () => {
    // Regression: the modules used to be rebuilt by hand without refClazzes, and the graph
    // node for a split module then crashed reading its reference class.
    const halves = [
      moduleFixture({ id: 'a', name: 'orders-a' }),
      moduleFixture({ id: 'b', name: 'orders-b' }),
    ];
    const fakeApi = new FakeAxiosAdapter().respondTo('POST', '/api/module/split', {
      newModules: halves,
    });
    axios.defaults.adapter = fakeApi.adapter;
    const onSplitRefresh = vi.fn();
    renderSidebar(onSplitRefresh);

    await userEvent.click(screen.getByText('orders'));
    fireEvent.contextMenu(screen.getByText('OrderRepository'));
    await userEvent.click(screen.getByText('Split'));

    await vi.waitFor(() => expect(onSplitRefresh).toHaveBeenCalledWith('m1', halves));
    expect(JSON.parse(String(fakeApi.requestsTo('POST', '/api/module/split')[0].body))).toEqual({
      moduleId: 'm1',
      classIds: ['c2'],
    });
  });
});

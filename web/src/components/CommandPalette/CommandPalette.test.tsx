// @vitest-environment jsdom
import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { type AxiosRequestConfig } from 'axios';
import { useMemo } from 'react';
import { Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { renderWithProviders } from '../../test/renderWithProviders';
import type { PaletteCommand } from './commandSearch';
import { CommandPalette } from './CommandPalette';
import { CommandPaletteProvider } from './CommandPaletteProvider';
import { useRegisterCommands } from './paletteContext';
import { PaletteTrigger } from './PaletteTrigger';

const runs: string[] = [];

// A page contributing its commands, as the Analyze page does.
function PageWithCommands() {
  const commands = useMemo<PaletteCommand[]>(
    () => [
      {
        id: 'c1',
        group: 'Classes',
        label: 'SalesReport',
        detail: 'bookstore/web · 50.00%',
        run: () => runs.push('SalesReport'),
      },
      {
        id: 'c2',
        group: 'Classes',
        label: 'CatalogController',
        detail: 'bookstore/web · 83.33%',
        run: () => runs.push('CatalogController'),
      },
      {
        id: 'a1',
        group: 'Actions',
        label: 'Export ACs',
        detail: 'PDF',
        run: () => runs.push('Export ACs'),
      },
    ],
    []
  );
  useRegisterCommands('page', commands);
  return <PaletteTrigger />;
}

describe('CommandPalette', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    runs.length = 0;
    fakeApi = new FakeAxiosAdapter().respondTo('GET', '/api/projects', {
      projects: [{ name: 'bookstore', source: null }],
    });
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  function renderPalette() {
    return renderWithProviders(
      <CommandPaletteProvider>
        <Routes>
          <Route path="/" element={<PageWithCommands />} />
          <Route path="/projects" element={<h1>All projects</h1>} />
          <Route path="/analyze/:projectName" element={<h1>Analyzing</h1>} />
        </Routes>
      </CommandPaletteProvider>
    );
  }

  function search(): HTMLElement {
    return screen.getByRole('combobox', { name: 'Search commands' });
  }

  it('opens on Ctrl+K with the search focused', async () => {
    renderPalette();

    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });

    expect(await screen.findByRole('dialog', { name: 'Command palette' })).toBeInTheDocument();
    await vi.waitFor(() => expect(search()).toHaveFocus());
  });

  it('opens from the header button too', async () => {
    renderPalette();

    await userEvent.click(screen.getByRole('button', { name: 'Open the command palette' }));

    expect(await screen.findByRole('dialog', { name: 'Command palette' })).toBeInTheDocument();
  });

  it('narrows the commands as one types, grouped', async () => {
    renderPalette();
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });

    await userEvent.type(search(), 'sales');

    const results = within(screen.getByRole('listbox', { name: 'Results' }));
    expect(results.getByRole('group', { name: 'Classes' })).toBeInTheDocument();
    expect(results.getAllByRole('option').map((option) => option.textContent)).toEqual([
      expect.stringContaining('SalesReport'),
    ]);
  });

  it('runs the command the arrow keys reach, on Enter', async () => {
    renderPalette();
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });

    await userEvent.type(search(), 'bookstore/web');
    expect(search()).toHaveAttribute('aria-activedescendant', screen.getAllByRole('option')[0].id);
    await userEvent.keyboard('{ArrowDown}');
    expect(screen.getAllByRole('option')[1]).toHaveAttribute('aria-selected', 'true');
    await userEvent.keyboard('{Enter}');

    expect(runs).toEqual(['CatalogController']);
  });

  it('runs a command that is clicked', async () => {
    renderPalette();
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });

    await userEvent.type(search(), 'export');
    await userEvent.click(screen.getByRole('option', { name: /Export ACs/ }));

    expect(runs).toEqual(['Export ACs']);
  });

  it('says when nothing matches', async () => {
    renderPalette();
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });

    await userEvent.type(search(), 'zzz');

    expect(screen.getByText('No command matches “zzz”.')).toBeInTheDocument();
  });

  it('goes to any page or project from anywhere', async () => {
    renderPalette();
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });

    await userEvent.type(search(), 'analyze bookstore');
    await userEvent.click(await screen.findByRole('option', { name: /Analyze bookstore/ }));

    expect(await screen.findByRole('heading', { name: 'Analyzing' })).toBeInTheDocument();
  });

  it('switches the theme', async () => {
    const { themeStore } = renderPalette();
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });

    await userEvent.type(search(), 'dark theme');
    await userEvent.keyboard('{Enter}');

    expect(themeStore.read()).toBe('dark');
  });
});

describe('CommandPalette dialog', () => {
  it('closes before it runs the chosen command, so the command acts on the page', async () => {
    const order: string[] = [];
    const command: PaletteCommand = {
      id: 'a1',
      group: 'Actions',
      label: 'Export ACs',
      run: () => order.push('run'),
    };
    renderWithProviders(
      <CommandPalette open commands={[command]} onClose={() => order.push('close')} />
    );

    await userEvent.click(await screen.findByRole('option', { name: /Export ACs/ }));

    expect(order).toEqual(['close', 'run']);
  });
});

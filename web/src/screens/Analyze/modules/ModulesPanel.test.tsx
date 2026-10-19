// @vitest-environment jsdom
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { type AxiosRequestConfig } from 'axios';
import { useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import type { ModuleData } from '../../../interface/ModuleData';
import { FakeAxiosAdapter } from '../../../test/fakes/FakeAxiosAdapter';
import { bookstoreModules } from '../../../test/fixtures/bookstoreFixture';
import { renderWithProviders } from '../../../test/renderWithProviders';
import type { ModuleChanges } from '../useModuleChanges';
import { useModuleOperations } from '../useModuleOperations';
import { ModulesPanel } from './ModulesPanel';

/** Records what the page would do after each change the API answered. */
class RecordedModuleChanges implements ModuleChanges {
  deleted = vi.fn();
  renamed = vi.fn();
  split = vi.fn();
  merged = vi.fn();
  referencesChanged = vi.fn();
  modeChanged = vi.fn();
}

function Harness(props: {
  modules: ModuleData[];
  changes: ModuleChanges;
  onProposeMerge: (module: ModuleData) => void;
}) {
  const operations = useModuleOperations(props.changes);
  // The page holds the module shown, so the command palette can open one.
  const [shownId, setShownId] = useState<string | null>(null);
  return (
    <ModulesPanel
      modules={props.modules}
      operations={operations}
      onProposeMerge={props.onProposeMerge}
      shownId={shownId}
      onShow={setShownId}
    />
  );
}

const modules = bookstoreModules();

describe('ModulesPanel', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];
  let changes: RecordedModuleChanges;
  let onProposeMerge: Mock<(module: ModuleData) => void>;

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter();
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
    changes = new RecordedModuleChanges();
    onProposeMerge = vi.fn<(module: ModuleData) => void>();
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  function renderPanel(shown: ModuleData[] = modules) {
    return renderWithProviders(
      <Harness modules={shown} changes={changes} onProposeMerge={onProposeMerge} />
    );
  }

  function panel() {
    return within(screen.getByRole('complementary', { name: 'Modules' }));
  }

  async function openDetailOf(name: string) {
    await userEvent.click(panel().getByRole('button', { name: new RegExp(`^${name}`) }));
  }

  it('lists every module with its reference class, similarity and violations', () => {
    renderPanel();

    const web = panel().getByRole('button', { name: /^bookstore\/web/ });
    expect(web).toHaveTextContent('CatalogController');
    expect(web).toHaveTextContent('75.00%');
    expect(web).toHaveTextContent('1 violation');
    expect(panel().getByText('4 modules')).toBeInTheDocument();
  });

  it('narrows the list as a name is typed', async () => {
    renderPanel();

    await userEvent.type(panel().getByRole('searchbox', { name: 'Search modules' }), 'pers');

    expect(panel().getAllByRole('button', { name: /^bookstore\// })).toHaveLength(1);
  });

  it('opens a module and goes back to the list', async () => {
    renderPanel();

    await openDetailOf('bookstore/web');
    expect(panel().getByRole('heading', { name: 'bookstore/web' })).toBeInTheDocument();

    await userEvent.click(panel().getByRole('button', { name: 'Back to all modules' }));
    expect(panel().getByText('4 modules')).toBeInTheDocument();
  });

  it('shows the module’s dependencies as chips, the violation marked', async () => {
    renderPanel();

    await openDetailOf('bookstore/web');

    const dependencies = within(panel().getByRole('region', { name: 'Dependencies' }));
    expect(dependencies.getByText('java.sql')).toBeInTheDocument();
    expect(dependencies.getByText(', violation')).toBeInTheDocument();
    expect(dependencies.getAllByText(', allowed')).toHaveLength(2);
  });

  it('switches a reference class to MUST, explaining the mode in force', async () => {
    fakeApi.respondTo('POST', '/api/module/ref-clazz-mode', modules[3]);
    renderPanel();
    await openDetailOf('bookstore/web');

    await userEvent.click(panel().getByRole('radio', { name: 'MUST' }));

    expect(
      JSON.parse(String(fakeApi.requestsTo('POST', '/api/module/ref-clazz-mode')[0].body))
    ).toEqual({
      moduleId: 'web',
      classId: 'web-c0',
      mode: 'MUST',
    });
    await vi.waitFor(() => expect(changes.modeChanged).toHaveBeenCalled());
  });

  it('offers what can be done with the classes selected, where the right-click menu hid it', async () => {
    fakeApi.respondTo('POST', '/api/module/ref-clazzes', modules[3]);
    renderPanel();
    await openDetailOf('bookstore/web');

    await userEvent.click(panel().getByRole('checkbox', { name: 'Select SalesReport' }));
    const toolbar = within(panel().getByRole('toolbar', { name: 'Selected classes' }));
    expect(toolbar.getByText('1 selected')).toBeInTheDocument();
    await userEvent.click(toolbar.getByRole('button', { name: 'Set as reference' }));

    expect(
      JSON.parse(String(fakeApi.requestsTo('POST', '/api/module/ref-clazzes')[0].body))
    ).toEqual({
      moduleId: 'web',
      classIds: ['web-sales'],
    });
    await vi.waitFor(() => expect(changes.referencesChanged).toHaveBeenCalled());
  });

  it('splits the selected classes into a new module', async () => {
    fakeApi.respondTo('POST', '/api/module/split', { newModules: modules.slice(0, 2) });
    renderPanel();
    await openDetailOf('bookstore/web');

    await userEvent.click(panel().getByRole('checkbox', { name: 'Select SalesReport' }));
    await userEvent.click(panel().getByRole('button', { name: 'Split' }));

    await vi.waitFor(() => expect(changes.split).toHaveBeenCalledWith('web', modules.slice(0, 2)));
  });

  it('renames a module in place: Enter saves, Escape cancels', async () => {
    fakeApi.respondTo('POST', '/api/module/rename', null);
    renderPanel();

    await userEvent.click(panel().getByRole('button', { name: 'Actions for bookstore/web' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Rename' }));
    const field = panel().getByRole('textbox', { name: 'New name for bookstore/web' });
    await userEvent.clear(field);
    await userEvent.type(field, 'bookstore/ui{Enter}');

    await vi.waitFor(() => expect(changes.renamed).toHaveBeenCalledWith('web', 'bookstore/ui'));
  });

  it('deletes a module after a confirmation that names it', async () => {
    fakeApi.respondTo('DELETE', '/api/module/delete', null);
    renderPanel();

    await userEvent.click(panel().getByRole('button', { name: 'Actions for bookstore/model' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Delete…' }));
    const dialog = within(await screen.findByRole('dialog'));
    expect(dialog.getByText('Delete bookstore/model?')).toBeInTheDocument();
    await userEvent.click(dialog.getByRole('button', { name: 'Delete module' }));

    await vi.waitFor(() => expect(changes.deleted).toHaveBeenCalledWith('model'));
  });

  it('offers merging without dragging', async () => {
    renderPanel();

    await userEvent.click(panel().getByRole('button', { name: 'Actions for bookstore/web' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Merge into…' }));

    expect(onProposeMerge).toHaveBeenCalledWith(modules[3]);
  });

  it('says what went wrong when the API refuses a change', async () => {
    fakeApi.failWith('POST', '/api/module/rename', 500, {});
    renderPanel();

    await userEvent.click(panel().getByRole('button', { name: 'Actions for bookstore/web' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Rename' }));
    await userEvent.type(
      panel().getByRole('textbox', { name: 'New name for bookstore/web' }),
      'x{Enter}'
    );

    expect(await screen.findByText(/Could not rename the module/)).toBeInTheDocument();
    expect(changes.renamed).not.toHaveBeenCalled();
  });
});

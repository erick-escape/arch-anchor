// @vitest-environment jsdom
import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { type AxiosRequestConfig } from 'axios';
import { Route, Routes, useParams } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { renderWithProviders } from '../../test/renderWithProviders';
import type {
  DirectoryEntry,
  DirectoryReader,
  FileEntry,
  FolderEntry,
} from './importBar/droppedFolder';
import HomePage from './index';

const BOOKSTORE_URL = 'https://github.com/erick-escape/arch-anchor';

// Stands in for the Analyze page, to see where the import led.
function AnalyzeProbe() {
  const { projectName } = useParams();
  return <h1>Analyzing {projectName}</h1>;
}

class FakeFileEntry implements FileEntry {
  readonly isFile = true;
  readonly isDirectory = false;
  constructor(readonly fullPath: string) {}
  file(onFile: (file: File) => void): void {
    onFile(new File(['class A {}'], this.fullPath.split('/').pop() ?? ''));
  }
}

class FakeDirectoryEntry implements DirectoryEntry {
  readonly isFile = false;
  readonly isDirectory = true;
  constructor(
    readonly fullPath: string,
    private readonly children: FolderEntry[]
  ) {}
  createReader(): DirectoryReader {
    let read = false;
    return {
      readEntries: (onEntries) => {
        onEntries(read ? [] : this.children);
        read = true;
      },
    };
  }
}

describe('HomePage', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter()
      .respondTo('GET', '/api/projects', { projects: [] })
      .respondTo('POST', '/api/upload', '')
      .respondTo('POST', '/api/projects/import', {
        name: 'bookstore',
        repositoryUrl: BOOKSTORE_URL,
        commitSha: '3ab2e30c0ffee',
      });
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  function renderHome() {
    return renderWithProviders(
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/analyze/:projectName" element={<AnalyzeProbe />} />
      </Routes>
    );
  }

  function importBar() {
    return within(screen.getByRole('form', { name: 'Import a project' }));
  }

  function sentImports(): unknown[] {
    return fakeApi
      .requestsTo('POST', '/api/projects/import')
      .map((request) => JSON.parse(String(request.body)));
  }

  it('leads with what the tool finds and where to start', () => {
    renderHome();

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Find the architecture already in your Java code.',
      })
    ).toBeInTheDocument();
    expect(importBar().getByRole('textbox', { name: 'GitHub repository URL' })).toHaveFocus();
  });

  it('recognises the repository, imports it and opens its analysis', async () => {
    renderHome();

    await userEvent.type(
      importBar().getByRole('textbox', { name: 'GitHub repository URL' }),
      BOOKSTORE_URL
    );
    expect(importBar().getByText('erick-escape/arch-anchor')).toBeInTheDocument();
    await userEvent.click(importBar().getByRole('button', { name: 'Analyze' }));

    expect(await screen.findByRole('heading', { name: 'Analyzing bookstore' })).toBeInTheDocument();
    expect(sentImports()).toEqual([{ repositoryUrl: BOOKSTORE_URL }]);
  });

  it('fills in the bookstore example in one click', async () => {
    renderHome();

    await userEvent.click(screen.getByRole('button', { name: 'Try it on the bookstore example' }));

    expect(importBar().getByRole('textbox', { name: 'GitHub repository URL' })).toHaveValue(
      BOOKSTORE_URL
    );
    expect(importBar().getByRole('button', { name: 'Options' })).toHaveAttribute(
      'aria-expanded',
      'true'
    );
    expect(importBar().getByRole('textbox', { name: 'Subdirectory' })).toHaveValue(
      'examples/bookstore'
    );
    await userEvent.click(importBar().getByRole('button', { name: 'Analyze' }));
    await screen.findByRole('heading', { name: 'Analyzing bookstore' });
    expect(sentImports()).toEqual([
      {
        repositoryUrl: BOOKSTORE_URL,
        subdirectory: 'examples/bookstore',
        projectName: 'bookstore',
      },
    ]);
  });

  it('keeps the options folded until asked, with the help each one needs', async () => {
    renderHome();

    expect(importBar().queryByRole('textbox', { name: 'Branch or tag' })).toBeNull();
    await userEvent.click(importBar().getByRole('button', { name: 'Options' }));

    expect(importBar().getByRole('textbox', { name: 'Branch or tag' })).toBeInTheDocument();
    expect(
      importBar().getByText('Only for private repositories. Used once to clone, never stored.')
    ).toBeInTheDocument();
  });

  it('says why an import failed, in the API’s words, and stays for a retry', async () => {
    fakeApi.failWith('POST', '/api/projects/import', 422, {
      detail: 'No src/ folder in owner/repo.',
    });
    renderHome();

    await userEvent.type(
      importBar().getByRole('textbox', { name: 'GitHub repository URL' }),
      'https://github.com/owner/repo'
    );
    await userEvent.click(importBar().getByRole('button', { name: 'Analyze' }));

    expect(await importBar().findByRole('alert')).toHaveTextContent(
      'No src/ folder in owner/repo.'
    );
    expect(importBar().getByRole('textbox', { name: 'GitHub repository URL' })).toHaveValue(
      'https://github.com/owner/repo'
    );
  });

  it('asks for a URL instead of sending an empty one', async () => {
    renderHome();

    await userEvent.click(importBar().getByRole('button', { name: 'Analyze' }));

    expect(importBar().getByRole('alert')).toHaveTextContent(
      'Paste the URL of a GitHub repository'
    );
    expect(sentImports()).toEqual([]);
  });

  it('uploads a chosen folder and opens its analysis', async () => {
    renderHome();
    const file = new File(['class Book {}'], 'Book.java');
    Object.defineProperty(file, 'webkitRelativePath', { value: 'bookstore/src/Book.java' });

    await userEvent.upload(screen.getByLabelText('Project folder'), file);

    expect(await screen.findByRole('heading', { name: 'Analyzing bookstore' })).toBeInTheDocument();
    expect(fakeApi.requestsTo('POST', '/api/upload')).toHaveLength(1);
  });

  it('takes a folder dropped anywhere on the page', async () => {
    renderHome();
    const dropped = new FakeDirectoryEntry('/bookstore', [new FakeFileEntry('/bookstore/pom.xml')]);
    const dataTransfer = { types: ['Files'], items: [{ webkitGetAsEntry: () => dropped }] };

    fireEvent.dragEnter(document.body, { dataTransfer });
    expect(screen.getByText('Drop the project folder')).toBeInTheDocument();
    fireEvent.drop(document.body, { dataTransfer });

    expect(await screen.findByRole('heading', { name: 'Analyzing bookstore' })).toBeInTheDocument();
  });

  it('turns through its claims with the controls', async () => {
    renderHome();
    const claims = within(screen.getByRole('region', { name: 'What the reference class shows' }));

    expect(claims.getByText('The architecture is already in your code.')).toBeInTheDocument();
    await userEvent.click(claims.getByRole('button', { name: 'Next claim' }));
    expect(claims.getByText('One class per module already carries its rules.')).toBeInTheDocument();
    await userEvent.click(claims.getByRole('button', { name: 'Claim 5 of 5' }));
    expect(claims.getByText('A start for the plan you never wrote.')).toBeInTheDocument();
    expect(claims.getByRole('button', { name: 'Claim 5 of 5' })).toHaveAttribute(
      'aria-current',
      'true'
    );
  });

  it('lists the projects already imported, each a click from its analysis', async () => {
    fakeApi.respondTo('GET', '/api/projects', {
      projects: [
        {
          name: 'bookstore',
          source: {
            repositoryUrl: BOOKSTORE_URL,
            subdirectory: 'examples/bookstore',
            ref: null,
            commitSha: '3ab2e30',
          },
        },
        { name: 'leona', source: null },
      ],
    });
    renderHome();

    const recent = within(await screen.findByRole('region', { name: 'Your projects' }));
    const bookstore = await recent.findByRole('link', { name: /bookstore/ });
    expect(bookstore).toHaveAttribute('href', '/analyze/bookstore');
    expect(bookstore).toHaveTextContent('erick-escape/arch-anchor · examples/bookstore');
    expect(recent.getByRole('link', { name: 'All projects' })).toHaveAttribute('href', '/projects');
  });

  it('cites the research and links its replication package', () => {
    renderHome();

    const research = within(screen.getByRole('region', { name: 'Research' }));
    expect(research.getByText(/ECSA 2026/)).toBeInTheDocument();
    expect(research.getByRole('link', { name: 'Replication package on Zenodo' })).toHaveAttribute(
      'href',
      'https://zenodo.org/records/20057336'
    );
  });
});

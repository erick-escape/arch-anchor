// @vitest-environment jsdom
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { type AxiosRequestConfig } from 'axios';
import { Route, Routes, useParams } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { renderWithProviders } from '../../test/renderWithProviders';
import ProjectsPage from './index';

function AnalyzeStub() {
  const { projectName } = useParams();
  return <p>analyzing {projectName}</p>;
}

function renderPage() {
  renderWithProviders(
    <Routes>
      <Route path="/projects" element={<ProjectsPage />} />
      <Route path="/analyze/:projectName" element={<AnalyzeStub />} />
      <Route path="/" element={<p>the import bar</p>} />
    </Routes>,
    { route: '/projects' }
  );
}

describe('ProjectsPage', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter()
      .respondTo('GET', '/api/projects', { projects: [{ name: 'shop' }, { name: 'bank' }] })
      .respondTo('DELETE', '/api/projects/shop', null);
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  it('shows each project as a card', async () => {
    renderPage();

    expect(await screen.findByRole('article', { name: 'shop' })).toBeInTheDocument();
    expect(screen.getByRole('article', { name: 'bank' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Projects', level: 1 })).toBeInTheDocument();
  });

  it('adds a project from the homepage’s import bar', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: 'Add a project' }));

    expect(screen.getByText('the import bar')).toBeInTheDocument();
  });

  it('opens a project for analysis', async () => {
    renderPage();

    const shop = await screen.findByRole('article', { name: 'shop' });
    await userEvent.click(within(shop).getByRole('button', { name: 'Analyze' }));

    expect(await screen.findByText('analyzing shop')).toBeInTheDocument();
  });

  it('deletes a project after a confirmation that names it', async () => {
    renderPage();

    const shop = await screen.findByRole('article', { name: 'shop' });
    await userEvent.click(within(shop).getByRole('button', { name: 'Delete…' }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Delete shop?');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete project' }));

    expect(fakeApi.requestsTo('DELETE', '/api/projects/shop')).toHaveLength(1);
  });

  it('invites the first import when there are no projects', async () => {
    fakeApi.respondTo('GET', '/api/projects', { projects: [] });
    renderPage();

    expect(await screen.findByRole('heading', { name: 'No projects yet' })).toBeInTheDocument();
  });

  it('says why the list is missing and offers to try again', async () => {
    fakeApi.failWith('GET', '/api/projects', 500, {});
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(/Could not load the projects/);
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });
});

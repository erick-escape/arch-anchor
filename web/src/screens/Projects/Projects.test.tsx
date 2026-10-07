// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import axios, { AxiosRequestConfig } from 'axios';
import ProjectsPage from './index';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';

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

  function renderPage() {
    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );
  }

  it('lists the projects the API returns', async () => {
    renderPage();

    expect(await screen.findByText('shop')).toBeInTheDocument();
    expect(screen.getByText('bank')).toBeInTheDocument();
  });

  it('deletes a project after confirmation', async () => {
    renderPage();

    const shopRow = (await screen.findByText('shop')).closest('tr');
    if (!shopRow) throw new Error('Expected the "shop" cell to sit inside a table row');
    await userEvent.click(within(shopRow).getByRole('button', { name: 'Delete' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Yes' }));

    expect(fakeApi.requestsTo('DELETE', '/api/projects/shop')).toHaveLength(1);
  });
});

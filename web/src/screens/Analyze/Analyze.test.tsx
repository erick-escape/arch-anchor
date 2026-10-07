// @vitest-environment jsdom
import { afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import axios, { AxiosRequestConfig } from 'axios';
import AnalyzePageWithProvider from './index';
import { api } from '../../services/api';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { installReactFlowJsdomShims } from '../../test/reactFlowJsdom';
import { moduleFixture } from '../../test/fixtures/moduleFixtures';
import { ProjectAnalyses } from '../../interface/ProjectAnalyses';

const analyses: ProjectAnalyses = {
  id: 'p1',
  projectName: 'shop',
  modulesList: [
    moduleFixture({ id: 'm1', name: 'orders' }),
    moduleFixture({ id: 'm2', name: 'billing' }),
  ],
  projectSimilarity: 0.6,
  architecturalConstraints: [],
};

const noRecommendations = {
  splits: [],
  merges: [],
  moves: [],
  violations: [],
  summary: { totalRecommendations: 0, highPriority: 0, mediumPriority: 0, lowPriority: 0 },
};

describe('AnalyzePage', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapters: AxiosRequestConfig['adapter'][];

  beforeAll(() => installReactFlowJsdomShims());

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter()
      .respondTo('POST', '/api/analyze', analyses)
      .respondTo('GET', '/api/recommendations', noRecommendations);
    // The recommendations client is its own axios instance, so it needs the fake too.
    realAdapters = [axios.defaults.adapter, api.defaults.adapter];
    axios.defaults.adapter = fakeApi.adapter;
    api.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    [axios.defaults.adapter, api.defaults.adapter] = realAdapters;
  });

  function renderPage() {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={['/analyze/shop']}>
          <Routes>
            <Route path="/analyze/:projectName" element={<AnalyzePageWithProvider />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  it('analyzes the project named in the URL', async () => {
    renderPage();

    expect(await screen.findAllByText('Ref Class:')).toHaveLength(2);
    expect(fakeApi.requestsTo('POST', '/api/analyze')[0].params).toEqual({ projectName: 'shop' });
    expect(screen.getByText(/Analyzed Modules for Project: shop/)).toBeInTheDocument();
  });

  it('draws a node and lists a sidebar card for every module', async () => {
    renderPage();

    await screen.findAllByText('Ref Class:');

    expect(screen.getAllByText('orders')).toHaveLength(2);
    expect(screen.getAllByText('billing')).toHaveLength(2);
  });
});

// @vitest-environment jsdom
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import axios, { AxiosRequestConfig } from 'axios';
import AnalyzePageWithProvider from './index';
import { api } from '../../services/api';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { installReactFlowJsdomShims } from '../../test/reactFlowJsdom';
import { fontAwesomeIcon } from '../../test/fontAwesomeIcon';
import { clazzFixture, moduleFixture } from '../../test/fixtures/moduleFixtures';
import {
  moveRecommendationFixture,
  recommendationsFixture,
} from '../../test/fixtures/recommendationFixtures';
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
    return render(
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

  // Re-analysing reads the sources again and discards the module operation (#19).
  it('shows the saved modules after a recommendation is applied, without analyzing again', async () => {
    fakeApi
      .respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ moves: [moveRecommendationFixture()] })
      )
      .respondTo('POST', '/api/recommendations/apply/move', {})
      .respondTo('GET', '/api/module', [
        moduleFixture({ id: 'm1', name: 'orders', similarity: 1 }),
        moduleFixture({ id: 'm2', name: 'billing' }),
      ]);
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Apply Recommendation' }));

    expect(await screen.findByText(/100\.00%/)).toBeInTheDocument();
    expect(fakeApi.requestsTo('POST', '/api/analyze')).toHaveLength(1);
  });

  // New weights re-elect the saved modules and re-rate the recommendations on the server
  // (#27); the page shows both without analyzing again, which would discard module operations.
  it('shows the re-elected modules and new recommendations after the weights change', async () => {
    const repository = clazzFixture({ id: 'c2', name: 'OrderRepository' });
    const equalWeights = {
      election: { similarity: 0.5, violation: 0.5 },
      recommendation: { similarity: 0.5, violation: 0.5 },
    };
    fakeApi
      .respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ moves: [moveRecommendationFixture()] })
      )
      .respondTo('GET', '/api/projects/shop/rating-weights', equalWeights)
      .respondTo('PUT', '/api/projects/shop/rating-weights', equalWeights)
      .respondTo('GET', '/api/module', [
        moduleFixture({ id: 'm1', name: 'orders', refClazzes: [repository] }),
        moduleFixture({ id: 'm2', name: 'billing' }),
      ]);
    const { container } = renderPage();
    await screen.findByRole('button', { name: 'Apply Recommendation' });
    fakeApi.respondTo('GET', '/api/recommendations', noRecommendations);

    await userEvent.click(fontAwesomeIcon(container, 'ellipsis-vertical', 0));
    await userEvent.click(screen.getByText('Settings'));
    fireEvent.keyDown(await screen.findByRole('slider', { name: 'Election: similarity weight' }), {
      key: 'ArrowRight',
      keyCode: 39,
      which: 39,
    });
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(
      await screen.findByText((_, element) => element?.textContent === 'Ref Class: OrderRepository')
    ).toBeInTheDocument();
    await vi.waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Apply Recommendation' })).toBeNull()
    );
    expect(fakeApi.requestsTo('PUT', '/api/projects/shop/rating-weights')).toHaveLength(1);
    expect(fakeApi.requestsTo('POST', '/api/analyze')).toHaveLength(1);
  });

  // A change made by hand left the recommendations of the previous structure on screen,
  // ready to be applied to modules that no longer exist (#28). Each test shows a card,
  // makes the change, and expects the page to ask for the recommendations again.
  describe('after a change made by hand in the sidebar', () => {
    beforeEach(() => {
      fakeApi.respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ moves: [moveRecommendationFixture()] })
      );
    });

    async function renderPageWithACard() {
      renderPage();
      await screen.findByRole('button', { name: 'Apply Recommendation' });
      fakeApi.respondTo('GET', '/api/recommendations', noRecommendations);
    }

    async function expectTheCardGone() {
      await vi.waitFor(() =>
        expect(screen.queryByRole('button', { name: 'Apply Recommendation' })).toBeNull()
      );
    }

    function sidebar(): HTMLElement {
      return screen.getByRole('complementary', { name: 'Modules' });
    }

    it('asks for the recommendations again after a rename', async () => {
      fakeApi.respondTo('POST', '/api/module/rename', null);
      await renderPageWithACard();

      await userEvent.click(fontAwesomeIcon(sidebar(), 'ellipsis-vertical', 0));
      await userEvent.click(screen.getByText('Rename'));
      await userEvent.clear(within(sidebar()).getByDisplayValue('orders'));
      await userEvent.type(within(sidebar()).getByRole('textbox'), 'checkout{Enter}');

      await expectTheCardGone();
    });

    it('asks for the recommendations again after a delete', async () => {
      fakeApi.respondTo('DELETE', '/api/module/delete', null);
      await renderPageWithACard();

      await userEvent.click(fontAwesomeIcon(sidebar(), 'ellipsis-vertical', 1));
      await userEvent.click(screen.getByText('Delete'));
      await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

      await expectTheCardGone();
    });

    it('asks for the recommendations again after a split', async () => {
      fakeApi.respondTo('POST', '/api/module/split', {
        newModules: [
          moduleFixture({ id: 'a', name: 'orders-a' }),
          moduleFixture({ id: 'b', name: 'orders-b' }),
        ],
      });
      await renderPageWithACard();

      await userEvent.click(within(sidebar()).getByText('orders'));
      fireEvent.contextMenu(within(sidebar()).getByText('OrderRepository'));
      await userEvent.click(screen.getByText('Split'));

      await expectTheCardGone();
    });

    it('shows the new reference class and asks for the recommendations again', async () => {
      const repository = clazzFixture({ id: 'c2', name: 'OrderRepository' });
      fakeApi.respondTo(
        'POST',
        '/api/module/ref-clazzes',
        moduleFixture({ id: 'm1', name: 'orders', refClazzes: [repository] })
      );
      await renderPageWithACard();

      await userEvent.click(within(sidebar()).getByText('orders'));
      fireEvent.contextMenu(within(sidebar()).getByText('OrderRepository'));
      await userEvent.click(screen.getByText('Set as Ref Classes'));

      await expectTheCardGone();
      expect(
        screen.getByText((_, element) => element?.textContent === 'Ref Class: OrderRepository')
      ).toBeInTheDocument();
    });
  });
});

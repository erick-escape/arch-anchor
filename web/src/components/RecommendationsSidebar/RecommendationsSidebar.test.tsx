// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosRequestConfig } from 'axios';
import RecommendationsSidebar from './index';
import { api } from '../../services/api';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import {
  moveRecommendationFixture,
  recommendationsFixture,
  splitRecommendationFixture,
  violationRecommendationFixture,
} from '../../test/fixtures/recommendationFixtures';

describe('RecommendationsSidebar', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter().respondTo(
      'GET',
      '/api/recommendations',
      recommendationsFixture({ splits: [splitRecommendationFixture()] })
    );
    realAdapter = api.defaults.adapter;
    api.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    api.defaults.adapter = realAdapter;
  });

  function renderSidebar(onRecommendationApplied: () => void = () => {}) {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <RecommendationsSidebar onRecommendationApplied={onRecommendationApplied} />
      </QueryClientProvider>
    );
  }

  async function clickApply() {
    await userEvent.click(await screen.findByRole('button', { name: 'Apply Recommendation' }));
  }

  it('applies a split by sending the ids of the classes to split off', async () => {
    fakeApi.respondTo('POST', '/api/recommendations/apply/split', { newModules: [] });
    renderSidebar();

    await clickApply();

    const sent = fakeApi.requestsTo('POST', '/api/recommendations/apply/split')[0];
    expect(JSON.parse(String(sent.body))).toEqual({ moduleId: 'm1', classIds: ['c9'] });
  });

  it('tells the page once the recommendation is applied', async () => {
    fakeApi.respondTo('POST', '/api/recommendations/apply/split', { newModules: [] });
    const onRecommendationApplied = vi.fn();
    renderSidebar(onRecommendationApplied);

    await clickApply();

    await waitFor(() => expect(onRecommendationApplied).toHaveBeenCalledOnce());
  });

  it('leaves the page alone when the backend rejects the apply', async () => {
    fakeApi.failWith('POST', '/api/recommendations/apply/split', 400, null);
    const onRecommendationApplied = vi.fn();
    renderSidebar(onRecommendationApplied);

    await clickApply();

    expect(await screen.findByText(/Failed to apply split/)).toBeInTheDocument();
    expect(onRecommendationApplied).not.toHaveBeenCalled();
  });

  // #48: violations have no rate; they were sorted among the refactorings at a made-up 0.3.
  describe('with refactorings and violations', () => {
    beforeEach(() => {
      fakeApi.respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({
          splits: [splitRecommendationFixture()],
          moves: [
            moveRecommendationFixture({
              metrics: { ...moveRecommendationFixture().metrics, rate: 0.1 },
            }),
          ],
          violations: [violationRecommendationFixture()],
        })
      );
    });

    it('lists the violations in a tab of their own', async () => {
      renderSidebar();

      expect(await screen.findByText('Split "shop/web"')).toBeInTheDocument();
      expect(screen.queryByText('Fix violation in "SalesReport"')).not.toBeInTheDocument();

      await userEvent.click(screen.getByRole('tab', { name: /Violations/ }));

      expect(await screen.findByText('Fix violation in "SalesReport"')).toBeInTheDocument();
    });

    it('names each tab with its number of cards', async () => {
      renderSidebar();

      expect(await screen.findByRole('tab', { name: /Refactorings\s*2/ })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Violations\s*1/ })).toBeInTheDocument();
    });

    it('counts the priorities of the refactorings only', async () => {
      renderSidebar();

      expect(await screen.findByLabelText('High priority')).toHaveTextContent('1');
      expect(screen.getByLabelText('Medium priority')).toHaveTextContent('0');
      expect(screen.getByLabelText('Low priority')).toHaveTextContent('1');
    });

    it('shows a violation without a priority', async () => {
      renderSidebar();

      await userEvent.click(await screen.findByRole('tab', { name: /Violations/ }));

      expect(await screen.findByText('Violation', { selector: '.ant-tag' })).toBeInTheDocument();
      expect(screen.queryByText('Medium', { selector: '.ant-tag' })).not.toBeInTheDocument();
    });
  });

  // #49: drawing every card at once froze the page for about 40 s on bytecode-viewer (1,593 cards).
  describe('with more cards than a page', () => {
    beforeEach(() => {
      const moves = Array.from({ length: 120 }, (_, index) =>
        moveRecommendationFixture({ classId: `c${index}`, className: `Report${index}` })
      );
      fakeApi.respondTo('GET', '/api/recommendations', recommendationsFixture({ moves }));
    });

    it('draws the first 50 cards', async () => {
      renderSidebar();

      expect(await screen.findAllByRole('button', { name: 'Apply Recommendation' })).toHaveLength(
        50
      );
    });

    it('draws the next 50 on request, then the rest', async () => {
      renderSidebar();

      await userEvent.click(await screen.findByRole('button', { name: 'Show 50 more of 70' }));
      expect(screen.getAllByRole('button', { name: 'Apply Recommendation' })).toHaveLength(100);

      await userEvent.click(screen.getByRole('button', { name: 'Show 20 more of 20' }));
      expect(screen.getAllByRole('button', { name: 'Apply Recommendation' })).toHaveLength(120);
      expect(screen.queryByRole('button', { name: /Show .* more/ })).not.toBeInTheDocument();
    });

    it('still names the tab with every card', async () => {
      renderSidebar();

      expect(await screen.findByRole('tab', { name: /Refactorings\s*120/ })).toBeInTheDocument();
    });
  });
});

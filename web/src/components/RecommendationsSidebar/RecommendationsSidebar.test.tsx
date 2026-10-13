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
  recommendationsFixture,
  splitRecommendationFixture,
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
});

import axios, { type AxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';
import { applyRecommendation, getRecommendations } from './recommendationsApi';

const noRecommendations = {
  splits: [],
  merges: [],
  moves: [],
  violations: [],
  summary: { totalRecommendations: 0, highPriority: 0, mediumPriority: 0, lowPriority: 0 },
};

// Like every other client, it calls the API on the page's own origin: the dev server proxies
// /api, and the packaged tool serves both halves on one port (#77). It used to call
// http://localhost:8080 whatever port the page came from.
describe('recommendationsApi', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter();
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  it('loads the recommendations from a relative path', async () => {
    fakeApi.respondTo('GET', '/api/recommendations', noRecommendations);

    expect(await getRecommendations()).toEqual(noRecommendations);
    expect(fakeApi.requests[0].url).toBe('/api/recommendations');
  });

  it('applies a split, a merge and a move through relative paths', async () => {
    fakeApi
      .respondTo('POST', '/api/recommendations/apply/split', {})
      .respondTo('POST', '/api/recommendations/apply/merge', {})
      .respondTo('POST', '/api/recommendations/apply/move', {});

    await applyRecommendation({ kind: 'split', moduleId: 'm1', classIds: ['c1'] });
    await applyRecommendation({ kind: 'merge', sourceId: 'm1', targetId: 'm2' });
    await applyRecommendation({
      kind: 'move',
      classId: 'c1',
      sourceModuleId: 'm1',
      targetModuleId: 'm2',
    });

    expect(fakeApi.requests.map((request) => request.url)).toEqual([
      '/api/recommendations/apply/split',
      '/api/recommendations/apply/merge',
      '/api/recommendations/apply/move',
    ]);
  });
});

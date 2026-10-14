import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import axios, { AxiosRequestConfig } from 'axios';
import {
  describeRatingWeightsFailure,
  getRatingWeights,
  updateRatingWeights,
} from './ratingWeightsApi';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';
import { RatingWeights } from '../interface/RatingWeights';

const EQUAL: RatingWeights = {
  election: { similarity: 0.5, violation: 0.5 },
  recommendation: { similarity: 0.5, violation: 0.5 },
};

const SIMILARITY_FIRST: RatingWeights = {
  election: { similarity: 0.9, violation: 0.1 },
  recommendation: { similarity: 0.5, violation: 0.5 },
};

describe('ratingWeightsApi', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter()
      .respondTo('GET', '/api/projects/shop/rating-weights', EQUAL)
      .respondTo('PUT', '/api/projects/shop/rating-weights', SIMILARITY_FIRST);
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  it("reads the project's weights", async () => {
    expect(await getRatingWeights('shop')).toEqual(EQUAL);
  });

  it('sends the new weights and returns those the API saved', async () => {
    const saved = await updateRatingWeights('shop', SIMILARITY_FIRST);

    const request = fakeApi.requestsTo('PUT', '/api/projects/shop/rating-weights')[0];
    expect(JSON.parse(String(request.body))).toEqual(SIMILARITY_FIRST);
    expect(saved).toEqual(SIMILARITY_FIRST);
  });

  it('keeps a project name with URL characters inside its path segment', async () => {
    fakeApi.respondTo('GET', '/api/projects/my%20shop/rating-weights', EQUAL);

    expect(await getRatingWeights('my shop')).toEqual(EQUAL);
  });

  it('describes a rejected change with the detail the API wrote', async () => {
    const detail = 'Invalid election weights. Expected two weights in [0, 1] summing to 1';
    fakeApi.failWith('PUT', '/api/projects/shop/rating-weights', 400, { status: 400, detail });

    const failure = await updateRatingWeights('shop', SIMILARITY_FIRST).catch(
      (error: unknown) => error
    );

    expect(describeRatingWeightsFailure(failure)).toBe(detail);
  });

  it('falls back to a generic description when the API sent no detail', () => {
    expect(describeRatingWeightsFailure(new Error('Network Error'))).toMatch(/could not save/i);
  });

  it('says what could not be done when the weights did not load', () => {
    expect(describeRatingWeightsFailure(new Error('Network Error'), 'load')).toMatch(
      /could not load/i
    );
  });
});

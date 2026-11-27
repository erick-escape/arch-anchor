// @vitest-environment jsdom
import axios, { type AxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';
import { analyzeProject } from './analysisApi';

describe('analyzeProject', () => {
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

  // The analysis page shows the sentence as it is (#87).
  it('fails with what the API says when the project has nothing to analyse', async () => {
    const detail = "Found no sources to analyse in 'notes'. Expected Java sources under src/main/.";
    fakeApi.failWith('POST', '/api/analyze', 422, { status: 422, detail });

    await expect(analyzeProject('notes')).rejects.toThrow(detail);
  });

  it('says to check the API when it answers nothing', async () => {
    fakeApi.failWith('POST', '/api/analyze', 500, {});

    await expect(analyzeProject('notes')).rejects.toThrow(
      'Could not analyze notes. Check that the API is running, then try again.'
    );
  });
});

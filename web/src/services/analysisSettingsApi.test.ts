import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import axios, { AxiosRequestConfig } from 'axios';
import {
  describeAnalysisSettingsFailure,
  getAnalysisSettings,
  updateAnalysisSettings,
} from './analysisSettingsApi';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';
import { AnalysisSettings } from '../interface/AnalysisSettings';

const SETTINGS_URL = '/api/projects/shop/analysis-settings';

const DEFAULTS: AnalysisSettings = {
  minSplitModuleClasses: 1,
  minSourceClassesAfterMove: 1,
  maxMoveTargetsPerClass: null,
  minClassDependencies: 0,
  minRecommendationRate: 0,
};

const STRICT: AnalysisSettings = {
  minSplitModuleClasses: 2,
  minSourceClassesAfterMove: 2,
  maxMoveTargetsPerClass: 1,
  minClassDependencies: 5,
  minRecommendationRate: 0.2,
};

describe('analysisSettingsApi', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter()
      .respondTo('GET', SETTINGS_URL, DEFAULTS)
      .respondTo('PUT', SETTINGS_URL, STRICT);
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  it("reads the project's settings", async () => {
    expect(await getAnalysisSettings('shop')).toEqual(DEFAULTS);
  });

  it('sends the new settings and returns those the API saved', async () => {
    const saved = await updateAnalysisSettings('shop', STRICT);

    const request = fakeApi.requestsTo('PUT', SETTINGS_URL)[0];
    expect(JSON.parse(String(request.body))).toEqual(STRICT);
    expect(saved).toEqual(STRICT);
  });

  it('keeps a project name with URL characters inside its path segment', async () => {
    fakeApi.respondTo('GET', '/api/projects/my%20shop/analysis-settings', DEFAULTS);

    expect(await getAnalysisSettings('my shop')).toEqual(DEFAULTS);
  });

  it('describes a rejected change with the detail the API wrote', async () => {
    const detail = 'Expected minSplitModuleClasses to be an integer >= 1, got 0';
    fakeApi.failWith('PUT', SETTINGS_URL, 400, { status: 400, detail });

    const failure = await updateAnalysisSettings('shop', STRICT).catch((error: unknown) => error);

    expect(describeAnalysisSettingsFailure(failure)).toBe(detail);
  });

  it('describes an unreachable API with a hint', () => {
    expect(describeAnalysisSettingsFailure(new Error('Network Error'), 'load')).toBe(
      'Could not load the settings. Check that the API is running and try again.'
    );
  });
});

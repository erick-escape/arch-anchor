import axios, { type AxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';
import { moduleFixture } from '../test/fixtures/moduleFixtures';
import {
  deleteModule,
  mergeModules,
  renameModule,
  setEnforceMode,
  setReferenceClasses,
  splitModule,
} from './moduleApi';

describe('moduleApi', () => {
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

  function bodyOf(method: string, url: string): unknown {
    return JSON.parse(String(fakeApi.requestsTo(method, url)[0].body));
  }

  it('renames and deletes with the parameters the API reads', async () => {
    fakeApi
      .respondTo('POST', '/api/module/rename', null)
      .respondTo('DELETE', '/api/module/delete', null);

    await renameModule('m1', 'checkout');
    await deleteModule('m1');

    expect(fakeApi.requestsTo('POST', '/api/module/rename')[0].params).toEqual({
      moduleId: 'm1',
      newName: 'checkout',
    });
    expect(fakeApi.requestsTo('DELETE', '/api/module/delete')[0].params).toEqual({
      moduleId: 'm1',
    });
  });

  it('splits, sets reference classes and an enforce mode with the bodies the API reads', async () => {
    const changed = moduleFixture();
    fakeApi
      .respondTo('POST', '/api/module/split', { newModules: [changed] })
      .respondTo('POST', '/api/module/ref-clazzes', changed)
      .respondTo('POST', '/api/module/ref-clazz-mode', changed);

    expect(await splitModule('m1', ['c2'])).toEqual([changed]);
    expect(await setReferenceClasses('m1', ['c2'])).toEqual(changed);
    expect(await setEnforceMode('m1', 'c1', 'MUST')).toEqual(changed);

    expect(bodyOf('POST', '/api/module/split')).toEqual({ moduleId: 'm1', classIds: ['c2'] });
    expect(bodyOf('POST', '/api/module/ref-clazzes')).toEqual({ moduleId: 'm1', classIds: ['c2'] });
    expect(bodyOf('POST', '/api/module/ref-clazz-mode')).toEqual({
      moduleId: 'm1',
      classId: 'c1',
      mode: 'MUST',
    });
  });

  it('merges the source into the target', async () => {
    fakeApi.respondTo('POST', '/api/module/merge', moduleFixture({ id: 'merged' }));

    expect((await mergeModules('m1', 'm2')).id).toBe('merged');
    expect(fakeApi.requestsTo('POST', '/api/module/merge')[0].params).toEqual({
      sourceId: 'm1',
      targetId: 'm2',
    });
  });

  it('turns a failure into a sentence naming what could not be done', async () => {
    fakeApi.failWith('POST', '/api/module/rename', 500, {});

    await expect(renameModule('m1', 'checkout')).rejects.toThrow(
      'Could not rename the module. Check that the API is running, then try again.'
    );
  });
});

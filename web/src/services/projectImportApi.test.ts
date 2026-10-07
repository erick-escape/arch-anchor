import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import axios, { AxiosRequestConfig } from 'axios';
import { describeImportFailure, importRepository } from './projectImportApi';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';

const IMPORTED = {
  name: 'spring-petclinic',
  repositoryUrl: 'https://github.com/spring-projects/spring-petclinic',
  commitSha: '4f2c9e1d7b3a5c8e0f6d2b4a9c1e7f3d5b8a0c2e',
};

describe('projectImportApi', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter().respondTo('POST', '/api/projects/import', IMPORTED);
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  function sentBody(): unknown {
    return JSON.parse(String(fakeApi.requestsTo('POST', '/api/projects/import')[0].body));
  }

  it('returns the imported project', async () => {
    const imported = await importRepository({ repositoryUrl: IMPORTED.repositoryUrl });

    expect(imported).toEqual(IMPORTED);
  });

  it('sends trimmed values and leaves out blank options', async () => {
    await importRepository({
      repositoryUrl: `  ${IMPORTED.repositoryUrl} `,
      ref: ' v2.1 ',
      subdirectory: '',
      projectName: '   ',
      accessToken: undefined,
    });

    expect(sentBody()).toEqual({ repositoryUrl: IMPORTED.repositoryUrl, ref: 'v2.1' });
  });

  it('describes a failure with the detail the API wrote', async () => {
    const detail = "A project named 'spring-petclinic' already exists";
    fakeApi.failWith('POST', '/api/projects/import', 409, { status: 409, detail });

    const failure = await importRepository({ repositoryUrl: IMPORTED.repositoryUrl }).catch(
      (error: unknown) => error
    );

    expect(describeImportFailure(failure)).toBe(detail);
  });

  it('falls back to a generic description when the API sent no detail', () => {
    expect(describeImportFailure(new Error('Network Error'))).toMatch(/could not import/i);
  });
});

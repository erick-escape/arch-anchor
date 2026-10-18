import axios, { type AxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';
import { deleteProject, listProjects } from './projectsApi';

describe('projectsApi', () => {
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

  it('lists the projects', async () => {
    fakeApi.respondTo('GET', '/api/projects', { projects: [{ name: 'bookstore' }] });

    expect(await listProjects()).toEqual([{ name: 'bookstore' }]);
  });

  it('deletes a project by its name, encoded for the URL', async () => {
    fakeApi.respondTo('DELETE', '/api/projects/my%20shop', null);

    await deleteProject('my shop');

    expect(fakeApi.requestsTo('DELETE', '/api/projects/my%20shop')).toHaveLength(1);
  });

  it('explains a failed load in words the user can act on', async () => {
    fakeApi.failWith('GET', '/api/projects', 500, {});

    await expect(listProjects()).rejects.toThrow(/Could not load the projects/);
  });
});

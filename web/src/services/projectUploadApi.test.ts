// @vitest-environment jsdom
import axios, { type AxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';
import { folderFilesOf, uploadProjectFolder, type FolderFile } from './projectUploadApi';

function folderFile(path: string): FolderFile {
  return { file: new File(['class A {}'], path.split('/').pop() ?? path), path };
}

describe('folderFilesOf', () => {
  it('keeps the path of each file a folder picker chose', () => {
    const file = new File([''], 'Book.java');
    Object.defineProperty(file, 'webkitRelativePath', { value: 'bookstore/src/Book.java' });

    expect(folderFilesOf([file])).toEqual([{ file, path: 'bookstore/src/Book.java' }]);
  });
});

describe('uploadProjectFolder', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter().respondTo('POST', '/api/upload', '');
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  it('sends every file under its path and names the project after the folder', async () => {
    const name = await uploadProjectFolder([
      folderFile('bookstore/pom.xml'),
      folderFile('bookstore/src/Book.java'),
    ]);

    expect(name).toBe('bookstore');
    const body = fakeApi.requestsTo('POST', '/api/upload')[0].body as FormData;
    expect(body.getAll('files').map((file) => (file as File).name)).toEqual([
      'bookstore/pom.xml',
      'bookstore/src/Book.java',
    ]);
  });

  it('refuses an empty folder without calling the API', async () => {
    await expect(uploadProjectFolder([])).rejects.toThrow('The folder is empty');
    expect(fakeApi.requests).toHaveLength(0);
  });

  it('says what went wrong when the API cannot be reached', async () => {
    fakeApi.failWith('POST', '/api/upload', 500, {});

    await expect(uploadProjectFolder([folderFile('bookstore/pom.xml')])).rejects.toThrow(
      'Could not upload the folder. Check that the API is running and try again.'
    );
  });
});

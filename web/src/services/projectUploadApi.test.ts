// @vitest-environment jsdom
import axios, { type AxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { FakeAxiosAdapter } from '../test/fakes/FakeAxiosAdapter';
import {
  FILES_PER_REQUEST,
  folderFilesOf,
  uploadProjectFolder,
  type FolderFile,
} from './projectUploadApi';

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

  it('sends only what the tool reads', async () => {
    await uploadProjectFolder([
      folderFile('shop/.env'),
      folderFile('shop/composer.json'),
      folderFile('shop/vendor/autoload.php'),
      folderFile('shop/vendor/acme/lib/Thing.php'),
      folderFile('shop/app/Models/Order.php'),
    ]);

    const body = fakeApi.requestsTo('POST', '/api/upload')[0].body as FormData;
    expect(body.getAll('files').map((file) => (file as File).name)).toEqual([
      'shop/composer.json',
      'shop/app/Models/Order.php',
    ]);
  });

  it('sends a large project in several requests, each under the server’s limit', async () => {
    const files = Array.from({ length: 2 * FILES_PER_REQUEST + 1 }, (_, index) =>
      folderFile(`shop/app/Model${index}.php`)
    );

    const name = await uploadProjectFolder(files);

    expect(name).toBe('shop');
    const sizes = fakeApi
      .requestsTo('POST', '/api/upload')
      .map((request) => (request.body as FormData).getAll('files').length);
    expect(sizes).toEqual([FILES_PER_REQUEST, FILES_PER_REQUEST, 1]);
  });

  it('refuses a folder with nothing to analyse without calling the API', async () => {
    const expected = 'Found nothing to analyse in the folder: expected .java or .php sources.';

    await expect(uploadProjectFolder([])).rejects.toThrow(expected);
    await expect(uploadProjectFolder([folderFile('notes/README.md')])).rejects.toThrow(expected);
    expect(fakeApi.requests).toHaveLength(0);
  });

  it('says what went wrong when the API cannot be reached', async () => {
    fakeApi.failWith('POST', '/api/upload', 500, {});

    await expect(uploadProjectFolder([folderFile('bookstore/pom.xml')])).rejects.toThrow(
      'Could not upload the folder. Check that the API is running and try again.'
    );
  });
});

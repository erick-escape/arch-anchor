// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DirectoryUploadForm from './DirectoryUploadForm';
import { FakeFetch } from '../../test/fakes/FakeFetch';

function fileInDirectory(relativePath: string, fileName: string): File {
  const file = new File(['class A {}'], fileName, { type: 'text/x-java' });
  // jsdom never sets webkitRelativePath; browsers do when the input has webkitdirectory.
  Object.defineProperty(file, 'webkitRelativePath', { value: relativePath });
  return file;
}

function directoryInput(): HTMLInputElement {
  const input = document.querySelector<HTMLInputElement>('input[type="file"]');
  if (!input)
    throw new Error('Expected DirectoryUploadForm to render an <input type="file">, found none');
  return input;
}

describe('DirectoryUploadForm', () => {
  let fakeFetch: FakeFetch;

  beforeEach(() => {
    fakeFetch = new FakeFetch(200);
    vi.stubGlobal('fetch', fakeFetch.fetch);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders a directory picker', () => {
    render(<DirectoryUploadForm onSuccess={() => {}} />);

    expect(directoryInput()).toHaveAttribute('webkitdirectory');
    expect(directoryInput()).toHaveAttribute('multiple');
  });

  it('uploads each file under its path relative to the chosen directory', async () => {
    const onSuccess = vi.fn();
    render(<DirectoryUploadForm onSuccess={onSuccess} />);

    await userEvent.upload(directoryInput(), [
      fileInDirectory('shop/src/Order.java', 'Order.java'),
      fileInDirectory('shop/src/repo/OrderRepository.java', 'OrderRepository.java'),
    ]);
    await userEvent.click(screen.getByRole('button', { name: 'Upload' }));

    await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledOnce());
    expect(fakeFetch.calls).toHaveLength(1);
    expect(fakeFetch.calls[0].url).toBe('/api/upload');
    const uploaded = (fakeFetch.calls[0].init?.body as FormData).getAll('files') as File[];
    expect(uploaded.map((file) => file.name)).toEqual([
      'shop/src/Order.java',
      'shop/src/repo/OrderRepository.java',
    ]);
  });

  it('does not call the API when no directory was chosen', async () => {
    render(<DirectoryUploadForm onSuccess={() => {}} />);

    await userEvent.click(screen.getByRole('button', { name: 'Upload' }));

    expect(fakeFetch.calls).toHaveLength(0);
  });
});

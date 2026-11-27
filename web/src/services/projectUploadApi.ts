import axios from 'axios';
import { analysableFilesOf } from './analysableFiles';

/** A file of an uploaded folder and its path inside it, e.g. "bookstore/src/Book.java". */
export interface FolderFile {
  file: File;
  path: string;
}

// The server takes at most 10,000 files in one request (Tomcat's limit on the parts of a
// multipart request) and 500 MB; a large project goes in several (#87).
export const FILES_PER_REQUEST = 1000;

/**
 * The files a folder picker (`<input webkitdirectory>`) chose, each under its path.
 *
 * @example folderFilesOf(Array.from(input.files ?? []));
 */
export function folderFilesOf(files: File[]): FolderFile[] {
  return files.map((file) => ({ file, path: file.webkitRelativePath }));
}

/**
 * Uploads what the tool reads of a project folder (`analysableFilesOf`); the server keeps each
 * file under its path, so the project takes the folder's name, which this returns. A failure
 * becomes an Error written for the user.
 *
 * @example const name = await uploadProjectFolder(folderFilesOf(files)); // 'bookstore'
 */
export async function uploadProjectFolder(files: FolderFile[]): Promise<string> {
  const analysable = analysableFilesOf(files);
  if (analysable.length === 0)
    throw new Error(
      'Found nothing to analyse in the folder: expected .java or .php sources. Choose the project’s root folder.'
    );
  for (let start = 0; start < analysable.length; start += FILES_PER_REQUEST) {
    await sendFiles(analysable.slice(start, start + FILES_PER_REQUEST));
  }
  return analysable[0].path.split('/')[0];
}

async function sendFiles(files: FolderFile[]): Promise<void> {
  const form = new FormData();
  files.forEach(({ file, path }) => form.append('files', file, path));
  try {
    await axios.post('/api/upload', form);
  } catch {
    throw new Error('Could not upload the folder. Check that the API is running and try again.');
  }
}

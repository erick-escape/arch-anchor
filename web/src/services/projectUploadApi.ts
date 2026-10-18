import axios from 'axios';

/** A file of an uploaded folder and its path inside it, e.g. "bookstore/src/Book.java". */
export interface FolderFile {
  file: File;
  path: string;
}

/**
 * The files a folder picker (`<input webkitdirectory>`) chose, each under its path.
 *
 * @example folderFilesOf(Array.from(input.files ?? []));
 */
export function folderFilesOf(files: File[]): FolderFile[] {
  return files.map((file) => ({ file, path: file.webkitRelativePath }));
}

/**
 * Uploads a project folder; the server keeps each file under its path, so the project takes
 * the folder's name, which this returns. A failure becomes an Error written for the user.
 *
 * @example const name = await uploadProjectFolder(folderFilesOf(files)); // 'bookstore'
 */
export async function uploadProjectFolder(files: FolderFile[]): Promise<string> {
  if (files.length === 0)
    throw new Error('The folder is empty: choose the folder that holds src/.');
  const form = new FormData();
  files.forEach(({ file, path }) => form.append('files', file, path));
  try {
    await axios.post('/api/upload', form);
  } catch {
    throw new Error('Could not upload the folder. Check that the API is running and try again.');
  }
  return files[0].path.split('/')[0];
}

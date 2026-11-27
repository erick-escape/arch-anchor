import {
  isAnalysableFile,
  isComposerVendor,
  isOutsideTheProject,
} from '../../../services/analysableFiles';
import type { FolderFile } from '../../../services/projectUploadApi';

// The parts of the browser's FileSystemEntry API a dropped folder is read with; the DOM's
// FileSystemFileEntry and FileSystemDirectoryEntry satisfy them.
export interface FolderEntry {
  readonly isFile: boolean;
  readonly isDirectory: boolean;
  /** "/bookstore/src/Book.java" for a file of the dropped folder "bookstore". */
  readonly fullPath: string;
}

export interface FileEntry extends FolderEntry {
  file(onFile: (file: File) => void, onError?: (error: DOMException) => void): void;
}

export interface DirectoryReader {
  readEntries(
    onEntries: (entries: FolderEntry[]) => void,
    onError?: (error: DOMException) => void
  ): void;
}

export interface DirectoryEntry extends FolderEntry {
  createReader(): DirectoryReader;
}

/**
 * The files the tool reads under the dropped entries, each with its path inside the drop (#87).
 * A drop hands over entries, not files, and a folder's entries arrive in batches until an empty
 * one. Third-party directories are never entered, so a project's node_modules/ or Composer's
 * vendor/ costs nothing to drop.
 *
 * @example await filesOfDroppedEntries([item.webkitGetAsEntry()]); // [{ file, path: 'bookstore/pom.xml' }, …]
 */
export async function filesOfDroppedEntries(entries: FolderEntry[]): Promise<FolderFile[]> {
  const nested = await Promise.all(entries.map(filesOfDropped));
  return nested.flat();
}

// What the user dropped is taken whatever its name; only what is inside it is pruned.
async function filesOfDropped(entry: FolderEntry): Promise<FolderFile[]> {
  if (!entry.isDirectory) return analysableFileOf(entry);
  return filesAmong(await allEntriesOf(entry as DirectoryEntry));
}

async function filesAmong(entries: FolderEntry[]): Promise<FolderFile[]> {
  const nested = await Promise.all(entries.map(filesOfEntry));
  return nested.flat();
}

async function filesOfEntry(entry: FolderEntry): Promise<FolderFile[]> {
  if (!entry.isDirectory) return analysableFileOf(entry);
  if (isOutsideTheProject(nameOf(entry))) return [];
  const entries = await allEntriesOf(entry as DirectoryEntry);
  const fileNames = entries.filter((child) => child.isFile).map(nameOf);
  if (isComposerVendor(nameOf(entry), fileNames)) return [];
  return filesAmong(entries);
}

// The file's contents are read only for what is uploaded.
async function analysableFileOf(entry: FolderEntry): Promise<FolderFile[]> {
  if (!entry.isFile || !isAnalysableFile(nameOf(entry))) return [];
  const file = await new Promise<File>((resolve, reject) =>
    (entry as FileEntry).file(resolve, reject)
  );
  return [{ file, path: entry.fullPath.replace(/^\//, '') }];
}

function nameOf(entry: FolderEntry): string {
  return entry.fullPath.split('/').pop() ?? entry.fullPath;
}

async function allEntriesOf(directory: DirectoryEntry): Promise<FolderEntry[]> {
  const reader = directory.createReader();
  const entries: FolderEntry[] = [];
  for (;;) {
    const batch = await new Promise<FolderEntry[]>((resolve, reject) =>
      reader.readEntries(resolve, reject)
    );
    if (batch.length === 0) return entries;
    entries.push(...batch);
  }
}

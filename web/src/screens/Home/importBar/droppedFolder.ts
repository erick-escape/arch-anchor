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
 * Every file under the dropped entries, each with its path inside the drop. A drop hands over
 * entries, not files, and a folder's entries arrive in batches until an empty one.
 *
 * @example await filesOfDroppedEntries([item.webkitGetAsEntry()]); // [{ file, path: 'bookstore/pom.xml' }, …]
 */
export async function filesOfDroppedEntries(entries: FolderEntry[]): Promise<FolderFile[]> {
  const nested = await Promise.all(entries.map(filesOfEntry));
  return nested.flat();
}

async function filesOfEntry(entry: FolderEntry): Promise<FolderFile[]> {
  if (entry.isDirectory) return filesOfDroppedEntries(await allEntriesOf(entry as DirectoryEntry));
  if (!entry.isFile) return [];
  const file = await new Promise<File>((resolve, reject) =>
    (entry as FileEntry).file(resolve, reject)
  );
  return [{ file, path: entry.fullPath.replace(/^\//, '') }];
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

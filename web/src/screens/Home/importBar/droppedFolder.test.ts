import { describe, expect, it } from 'vitest';
import {
  filesOfDroppedEntries,
  type DirectoryEntry,
  type DirectoryReader,
  type FileEntry,
  type FolderEntry,
} from './droppedFolder';

/** A dropped file, as the browser's FileSystemFileEntry presents it. */
class FakeFileEntry implements FileEntry {
  readonly isFile = true;
  readonly isDirectory = false;
  constructor(readonly fullPath: string) {}
  file(onFile: (file: File) => void): void {
    onFile(new File([''], this.fullPath.split('/').pop() ?? ''));
  }
}

/** A dropped folder; hands out its entries in batches, as Chrome does (100 at a time). */
class FakeDirectoryEntry implements DirectoryEntry {
  readonly isFile = false;
  readonly isDirectory = true;
  constructor(
    readonly fullPath: string,
    private readonly children: FolderEntry[],
    private readonly batch = 100
  ) {}
  createReader(): DirectoryReader {
    let next = 0;
    return {
      readEntries: (onEntries) => {
        onEntries(this.children.slice(next, next + this.batch));
        next += this.batch;
      },
    };
  }
}

describe('filesOfDroppedEntries', () => {
  it('walks a dropped folder and keeps each file’s path inside it', async () => {
    const dropped = new FakeDirectoryEntry('/bookstore', [
      new FakeFileEntry('/bookstore/pom.xml'),
      new FakeDirectoryEntry('/bookstore/src', [new FakeFileEntry('/bookstore/src/Book.java')]),
    ]);

    const files = await filesOfDroppedEntries([dropped]);

    expect(files.map((file) => file.path)).toEqual([
      'bookstore/pom.xml',
      'bookstore/src/Book.java',
    ]);
  });

  it('reads a folder until it has no more entries, not just the first batch', async () => {
    const many = Array.from({ length: 5 }, (_, index) => new FakeFileEntry(`/p/F${index}.java`));

    const files = await filesOfDroppedEntries([new FakeDirectoryEntry('/p', many, 2)]);

    expect(files).toHaveLength(5);
  });
});

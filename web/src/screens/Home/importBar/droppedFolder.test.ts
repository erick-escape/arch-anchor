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

/** A folder the walk must not enter: it fails the test if its entries are asked for. */
class UnenterableDirectoryEntry implements DirectoryEntry {
  readonly isFile = false;
  readonly isDirectory = true;
  constructor(readonly fullPath: string) {}
  createReader(): DirectoryReader {
    throw new Error(`${this.fullPath} was entered, and it holds no code of the project's own`);
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

  it('leaves out the files the tool does not read', async () => {
    const dropped = new FakeDirectoryEntry('/shop', [
      new FakeFileEntry('/shop/.env'),
      new FakeFileEntry('/shop/composer.json'),
      new FakeFileEntry('/shop/package.json'),
      new FakeDirectoryEntry('/shop/app', [
        new FakeFileEntry('/shop/app/Order.php'),
        new FakeFileEntry('/shop/app/logo.png'),
      ]),
    ]);

    const files = await filesOfDroppedEntries([dropped]);

    expect(files.map((file) => file.path)).toEqual(['shop/composer.json', 'shop/app/Order.php']);
  });

  it('never enters node_modules or a directory whose name starts with a dot', async () => {
    const dropped = new FakeDirectoryEntry('/shop', [
      new UnenterableDirectoryEntry('/shop/node_modules'),
      new UnenterableDirectoryEntry('/shop/.git'),
      new FakeFileEntry('/shop/index.php'),
    ]);

    const files = await filesOfDroppedEntries([dropped]);

    expect(files.map((file) => file.path)).toEqual(['shop/index.php']);
  });

  it('reads only the top of Composer’s vendor/, to recognise it by its autoloader', async () => {
    const dropped = new FakeDirectoryEntry('/shop', [
      new FakeDirectoryEntry('/shop/vendor', [
        new FakeFileEntry('/shop/vendor/autoload.php'),
        new UnenterableDirectoryEntry('/shop/vendor/laravel'),
      ]),
      new FakeDirectoryEntry('/shop/app', [
        new FakeDirectoryEntry('/shop/app/vendor', [
          new FakeFileEntry('/shop/app/vendor/Supplier.php'),
        ]),
      ]),
    ]);

    const files = await filesOfDroppedEntries([dropped]);

    expect(files.map((file) => file.path)).toEqual(['shop/app/vendor/Supplier.php']);
  });

  it('takes the dropped folder whatever its name', async () => {
    const dropped = new FakeDirectoryEntry('/.shop', [new FakeFileEntry('/.shop/Order.java')]);

    expect(await filesOfDroppedEntries([dropped])).toHaveLength(1);
  });
});

import { describe, expect, it } from 'vitest';
import {
  analysableFilesOf,
  isAnalysableFile,
  isComposerVendor,
  isOutsideTheProject,
} from './analysableFiles';
import type { FolderFile } from './projectUploadApi';

function folderFiles(...paths: string[]): FolderFile[] {
  return paths.map((path) => ({ file: new File([''], path.split('/').pop() ?? path), path }));
}

describe('isAnalysableFile', () => {
  it('takes the sources of every language the tool reads, whatever the case of the extension', () => {
    expect(['Order.java', 'Order.php', 'legacy.PHP', 'Shop.JAVA'].map(isAnalysableFile)).toEqual([
      true,
      true,
      true,
      true,
    ]);
  });

  it('takes the manifests that say where the sources are or what they use', () => {
    const manifests = ['pom.xml', 'build.gradle', 'build.gradle.kts', 'composer.json'];

    expect(manifests.map(isAnalysableFile)).toEqual([true, true, true, true]);
  });

  it('leaves everything else on the user’s machine', () => {
    const others = ['.env', 'composer.lock', 'app.blade.php.bak', 'logo.png', 'application.yml'];

    expect(others.map(isAnalysableFile)).toEqual([false, false, false, false, false]);
  });
});

describe('isOutsideTheProject', () => {
  it('is true of tooling and of what npm installed', () => {
    expect(['.git', '.idea', 'node_modules'].map(isOutsideTheProject)).toEqual([true, true, true]);
  });

  it('is false of the project’s own directories', () => {
    expect(['src', 'app', 'vendor'].map(isOutsideTheProject)).toEqual([false, false, false]);
  });
});

describe('isComposerVendor', () => {
  it('is true of a vendor directory holding Composer’s autoloader', () => {
    expect(isComposerVendor('vendor', ['autoload.php'])).toBe(true);
  });

  it('is false of a vendor directory of the project’s own, or of another name', () => {
    expect(isComposerVendor('vendor', ['Supplier.php'])).toBe(false);
    expect(isComposerVendor('lib', ['autoload.php'])).toBe(false);
  });
});

describe('analysableFilesOf', () => {
  it('keeps the sources and manifests of a picked folder, under their paths', () => {
    const picked = folderFiles(
      'shop/composer.json',
      'shop/.env',
      'shop/app/Models/Order.php',
      'shop/resources/js/app.js',
      'shop/pom.xml'
    );

    expect(analysableFilesOf(picked).map((file) => file.path)).toEqual([
      'shop/composer.json',
      'shop/app/Models/Order.php',
      'shop/pom.xml',
    ]);
  });

  it('leaves out what Composer and npm installed and every dot-directory', () => {
    const picked = folderFiles(
      'shop/app/Models/Order.php',
      'shop/vendor/autoload.php',
      'shop/vendor/laravel/framework/src/Foundation/Application.php',
      'shop/node_modules/some-package/build.php',
      'shop/.git/hooks/pre-commit.php',
      'shop/app/.cache/Compiled.php'
    );

    expect(analysableFilesOf(picked).map((file) => file.path)).toEqual([
      'shop/app/Models/Order.php',
    ]);
  });

  it('keeps a vendor directory that Composer did not write', () => {
    const picked = folderFiles('shop/app/vendor/Supplier.php', 'shop/vendor/Catalogue.php');

    expect(analysableFilesOf(picked)).toHaveLength(2);
  });

  // The user chose it; its name says nothing about whose code it is.
  it('takes the chosen folder whatever its name', () => {
    expect(analysableFilesOf(folderFiles('.shop/src/Order.java'))).toHaveLength(1);
  });
});

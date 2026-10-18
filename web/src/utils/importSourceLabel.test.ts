import { describe, expect, it } from 'vitest';
import { importSourceLabelOf } from './importSourceLabel';

const BOOKSTORE = {
  repositoryUrl: 'https://github.com/erick-escape/arch-anchor',
  subdirectory: 'examples/bookstore',
  ref: null,
  commitSha: '3ab2e30c0ffee',
};

describe('importSourceLabelOf', () => {
  it('names the repository and the folder imported from it', () => {
    expect(importSourceLabelOf(BOOKSTORE)).toBe('erick-escape/arch-anchor · examples/bookstore');
  });

  it('adds the branch or tag when one was asked for', () => {
    expect(importSourceLabelOf({ ...BOOKSTORE, subdirectory: null, ref: 'v1.2' })).toBe(
      'erick-escape/arch-anchor@v1.2'
    );
  });

  it('says when no repository is recorded, as for an upload or an import older than #66', () => {
    expect(importSourceLabelOf(null)).toBe('No repository recorded');
    expect(importSourceLabelOf(undefined)).toBe('No repository recorded');
  });
});

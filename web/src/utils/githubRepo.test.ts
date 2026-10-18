import { describe, expect, it } from 'vitest';
import { githubRepoOf } from './githubRepo';

describe('githubRepoOf', () => {
  it('names the repository a GitHub URL points at', () => {
    expect(githubRepoOf('https://github.com/erick-escape/arch-anchor')).toBe(
      'erick-escape/arch-anchor'
    );
  });

  it('ignores a .git suffix and anything after the repository', () => {
    expect(githubRepoOf('https://github.com/owner/repo.git')).toBe('owner/repo');
    expect(githubRepoOf('https://github.com/owner/repo/tree/main/examples')).toBe('owner/repo');
  });

  it('recognises nothing in a URL that is not a GitHub repository', () => {
    expect(githubRepoOf('')).toBeNull();
    expect(githubRepoOf('https://github.com/owner')).toBeNull();
    expect(githubRepoOf('https://gitlab.com/owner/repo')).toBeNull();
  });
});

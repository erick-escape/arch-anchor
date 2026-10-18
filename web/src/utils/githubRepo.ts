// The owner and the repository, as GitHub allows them; whatever follows (a .git suffix, a
// tree/branch path) is not part of the name.
const GITHUB_REPOSITORY = /github\.com\/([A-Za-z0-9_.-]+)\/([A-Za-z0-9_.-]+)/;

/**
 * The "owner/repo" a GitHub URL points at, shown as a chip once the import bar recognises it;
 * null for anything else.
 *
 * @example githubRepoOf('https://github.com/erick-escape/arch-anchor.git'); // 'erick-escape/arch-anchor'
 */
export function githubRepoOf(url: string): string | null {
  const match = GITHUB_REPOSITORY.exec(url);
  if (!match) return null;
  return `${match[1]}/${match[2].replace(/\.git$/, '')}`;
}

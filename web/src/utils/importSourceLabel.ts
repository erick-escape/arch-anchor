import type { ImportSource } from '../interface/Project';
import { githubRepoOf } from './githubRepo';

/**
 * Where a project came from, in one line for its card: the repository, at its branch or tag
 * when one was asked for, then the folder imported from it. Without a source the project was
 * uploaded, or imported before sources were kept (#66), so the card says only that none is
 * recorded.
 *
 * @example importSourceLabelOf(source); // 'erick-escape/arch-anchor · examples/bookstore'
 */
export function importSourceLabelOf(source: ImportSource | null | undefined): string {
  if (!source) return 'No repository recorded';
  const repository = githubRepoOf(source.repositoryUrl) ?? source.repositoryUrl;
  const at = source.ref ? `${repository}@${source.ref}` : repository;
  return source.subdirectory ? `${at} · ${source.subdirectory}` : at;
}

import { useState } from 'react';
import type { RepositoryImportRequest } from '../../../interface/Project';

export const BOOKSTORE_EXAMPLE: RepositoryImportRequest = {
  repositoryUrl: 'https://github.com/erick-escape/arch-anchor',
  subdirectory: 'examples/bookstore',
  projectName: 'bookstore',
};

const EMPTY: RepositoryImportRequest = { repositoryUrl: '' };

export interface ImportFields {
  request: RepositoryImportRequest;
  set: (changes: Partial<RepositoryImportRequest>) => void;
  optionsOpen: boolean;
  toggleOptions: () => void;
  /** Fills in the bookstore example and opens the options, so its subdirectory shows. */
  fillBookstore: () => void;
}

/**
 * What the import bar holds: the repository URL and the options behind the Options toggle.
 *
 * @example const fields = useImportFields(); fields.set({ repositoryUrl: url });
 */
export function useImportFields(): ImportFields {
  const [request, setRequest] = useState(EMPTY);
  const [optionsOpen, setOptionsOpen] = useState(false);
  return {
    request,
    set: (changes) => setRequest((current) => ({ ...current, ...changes })),
    optionsOpen,
    toggleOptions: () => setOptionsOpen((open) => !open),
    fillBookstore: () => {
      setRequest(BOOKSTORE_EXAMPLE);
      setOptionsOpen(true);
    },
  };
}

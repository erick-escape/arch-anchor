import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../../../components/feedback/useToast';
import type { RepositoryImportRequest } from '../../../interface/Project';
import { describeImportFailure, importRepository } from '../../../services/projectImportApi';
import { uploadProjectFolder, type FolderFile } from '../../../services/projectUploadApi';
import { githubRepoOf } from '../../../utils/githubRepo';

export type ImportSource =
  | { kind: 'repository'; request: RepositoryImportRequest }
  | { kind: 'folder'; files: FolderFile[] };

export interface ProjectImport {
  start: (source: ImportSource) => void;
  /** What is being imported, e.g. "erick-escape/arch-anchor"; null when nothing is. */
  importing: string | null;
  /** Why the last import failed, written for the user; null after a success or a new start. */
  failure: string | null;
  /** Clears the failure, as soon as the user changes what failed. */
  dismiss: () => void;
}

/**
 * Imports a project from GitHub or from a folder, then opens its analysis, which analyzes it.
 *
 * @example const projectImport = useProjectImport(); projectImport.start({ kind: 'folder', files });
 */
export function useProjectImport(): ProjectImport {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const mutation = useMutation({
    mutationFn: (source: ImportSource) => importedNameOf(source, toast.success),
    onSuccess: (name) => {
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      navigate(`/analyze/${encodeURIComponent(name)}`);
    },
  });
  const source = mutation.variables;
  return {
    start: (next) => mutation.mutate(next),
    importing: mutation.isPending && source ? labelOf(source) : null,
    failure: mutation.isError && source ? failureOf(source, mutation.error) : null,
    dismiss: () => {
      if (mutation.isError) mutation.reset();
    },
  };
}

async function importedNameOf(
  source: ImportSource,
  report: (text: string) => void
): Promise<string> {
  if (source.kind === 'folder') {
    const name = await uploadProjectFolder(source.files);
    report(`Uploaded ${name}`);
    return name;
  }
  const imported = await importRepository(source.request);
  report(`Imported ${imported.name} at ${imported.commitSha.slice(0, 7)}`);
  return imported.name;
}

function labelOf(source: ImportSource): string {
  if (source.kind === 'folder') return source.files[0]?.path.split('/')[0] ?? 'the folder';
  return githubRepoOf(source.request.repositoryUrl) ?? source.request.repositoryUrl;
}

function failureOf(source: ImportSource, error: Error): string {
  return source.kind === 'repository' ? describeImportFailure(error) : error.message;
}

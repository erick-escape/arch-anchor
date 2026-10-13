import axios from 'axios';
import { ImportedProject, ProblemDetail, RepositoryImportRequest } from '../interface/Project';

const OPTIONAL_FIELDS = ['ref', 'subdirectory', 'projectName', 'accessToken'] as const;

/**
 * Creates a project from a GitHub repository. Uses a relative URL like the projects page, so
 * the request goes through the Vite proxy.
 *
 * @example
 * const imported = await importRepository({ repositoryUrl: 'https://github.com/owner/repo' });
 */
export const importRepository = async (
  request: RepositoryImportRequest
): Promise<ImportedProject> => {
  const response = await axios.post<ImportedProject>(
    '/api/projects/import',
    withoutBlankFields(request)
  );
  return response.data;
};

/**
 * Turns a failed import into a sentence for the user: the API's own explanation when it sent
 * one, a generic hint otherwise.
 *
 * @example
 * message.error(describeImportFailure(error));
 */
export function describeImportFailure(error: unknown): string {
  if (axios.isAxiosError<ProblemDetail>(error) && error.response?.data?.detail) {
    return error.response.data.detail;
  }
  return 'Could not import the repository. Check that the API is running and try again.';
}

function withoutBlankFields(request: RepositoryImportRequest): RepositoryImportRequest {
  const body: RepositoryImportRequest = { repositoryUrl: request.repositoryUrl.trim() };
  for (const field of OPTIONAL_FIELDS) {
    const value = request[field]?.trim();
    if (value) body[field] = value;
  }
  return body;
}

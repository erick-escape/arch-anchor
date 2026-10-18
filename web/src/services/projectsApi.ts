import axios from 'axios';
import type { ProjectSummary, ProjectsListResponse } from '../interface/Project';
import { problemDetailOf } from './problemDetail';

function failure(error: unknown, action: string): Error {
  return new Error(
    problemDetailOf(error) ?? `Could not ${action}. Check that the API is running and try again.`
  );
}

/**
 * The imported projects. A failure becomes an Error whose message is written for the user.
 *
 * @example const projects = await listProjects();
 */
export async function listProjects(): Promise<ProjectSummary[]> {
  try {
    const response = await axios.get<ProjectsListResponse>('/api/projects');
    return response.data.projects;
  } catch (error) {
    throw failure(error, 'load the projects');
  }
}

/**
 * Removes a project's sources and saved analysis from the server.
 *
 * @example await deleteProject('bookstore');
 */
export async function deleteProject(name: string): Promise<void> {
  try {
    await axios.delete(`/api/projects/${encodeURIComponent(name)}`);
  } catch (error) {
    throw failure(error, `delete ${name}`);
  }
}

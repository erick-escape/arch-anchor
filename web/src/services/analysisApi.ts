import axios from 'axios';
import type { ModuleData } from '../interface/ModuleData';
import type { ProjectAnalyses } from '../interface/ProjectAnalyses';
import { problemDetailOf } from './problemDetail';

function failure(error: unknown, action: string): Error {
  return new Error(
    problemDetailOf(error) ?? `Could not ${action}. Check that the API is running, then try again.`
  );
}

/**
 * Analyzes a project from its sources: parse, elect the reference classes, run the analyses.
 *
 * @example const { modulesList } = await analyzeProject('bookstore');
 */
export async function analyzeProject(projectName: string): Promise<ProjectAnalyses> {
  try {
    const response = await axios.post<ProjectAnalyses>('/api/analyze', null, {
      params: { projectName },
    });
    return response.data;
  } catch (error) {
    throw failure(error, `analyze ${projectName}`);
  }
}

/**
 * The modules as the API saved them after a change (an applied recommendation, new weights).
 * Analyzing again would read the sources and discard the change (#19).
 *
 * @example setModules(await savedModules());
 */
export async function savedModules(): Promise<ModuleData[]> {
  try {
    const response = await axios.get<ModuleData[]>('/api/module');
    return response.data;
  } catch (error) {
    throw failure(error, 'reload the modules');
  }
}

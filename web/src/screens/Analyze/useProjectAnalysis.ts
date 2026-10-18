import { useCallback, useEffect, useRef, useState } from 'react';
import type { ModuleData } from '../../interface/ModuleData';
import { analyzeProject, savedModules } from '../../services/analysisApi';

export type AnalysisStatus = 'analyzing' | 'ready' | 'failed';

interface AnalysisState {
  status: AnalysisStatus;
  modules: ModuleData[];
  error?: string;
}

export interface ProjectAnalysis extends AnalysisState {
  /** Changes the modules on screen after an operation the API answered. */
  setModules: (update: (modules: ModuleData[]) => ModuleData[]) => void;
  retry: () => void;
  /** Shows the modules the API saved, after it applied a recommendation or new weights. */
  reloadSavedModules: () => Promise<void>;
}

/**
 * Analyzes the project once per visit and keeps its modules. StrictMode runs the effect twice;
 * the ref keeps that to one request (#19). A failure is kept, with its reason, for a retry.
 *
 * @example const analysis = useProjectAnalysis('bookstore');
 */
export function useProjectAnalysis(projectName: string): ProjectAnalysis {
  const [state, setState] = useState<AnalysisState>({ status: 'analyzing', modules: [] });
  const [attempt, setAttempt] = useState(0);
  const started = useRef<string | null>(null);
  useEffect(() => {
    const key = `${projectName}#${attempt}`;
    if (started.current === key) return;
    started.current = key;
    setState({ status: 'analyzing', modules: [] });
    analyzeProject(projectName)
      .then((analyses) => setState({ status: 'ready', modules: analyses.modulesList }))
      .catch((error: Error) => setState({ status: 'failed', modules: [], error: error.message }));
  }, [projectName, attempt]);
  const setModules = useCallback((update: (modules: ModuleData[]) => ModuleData[]) => {
    setState((current) => ({ ...current, modules: update(current.modules) }));
  }, []);
  const reloadSavedModules = useCallback(async () => {
    const modules = await savedModules();
    setState((current) => ({ ...current, modules }));
  }, []);
  const retry = useCallback(() => setAttempt((count) => count + 1), []);
  return { ...state, setModules, retry, reloadSavedModules };
}

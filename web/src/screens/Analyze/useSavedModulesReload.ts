import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
import { useToast } from '../../components/feedback/useToast';
import type { ProjectAnalysis } from './useProjectAnalysis';

export interface SavedModulesReload {
  /** Counts the reloads; as the modules panel's key, it makes the panel start over. */
  reloads: number;
  reload: () => void;
}

/**
 * Shows the modules as the API saved them, after it applied a recommendation or re-elected them
 * with new weights (#27), and asks for the recommendations again, which the API re-rated.
 * Analyzing again would read the sources and discard the change (#19).
 */
export function useSavedModulesReload(analysis: ProjectAnalysis): SavedModulesReload {
  const [reloads, setReloads] = useState(0);
  const queryClient = useQueryClient();
  const toast = useToast();
  const { reloadSavedModules } = analysis;
  const reload = useCallback(() => {
    reloadSavedModules()
      .then(() => {
        setReloads((count) => count + 1);
        void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      })
      .catch((error: Error) => toast.error(error.message));
  }, [reloadSavedModules, queryClient, toast]);
  return { reloads, reload };
}

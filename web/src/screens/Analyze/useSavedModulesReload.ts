import { useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { useToast } from '../../components/feedback/useToast';
import type { ProjectAnalysis } from './useProjectAnalysis';

/**
 * Shows the modules as the API saved them, after it applied a recommendation or re-elected them
 * with new weights (#27), and asks for the recommendations again, which the API re-rated.
 * Analyzing again would read the sources and discard the change (#19).
 */
export function useSavedModulesReload(analysis: ProjectAnalysis): () => void {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { reloadSavedModules } = analysis;
  const reload = useCallback(() => {
    reloadSavedModules()
      .then(() => void queryClient.invalidateQueries({ queryKey: ['recommendations'] }))
      .catch((error: Error) => toast.error(error.message));
  }, [reloadSavedModules, queryClient, toast]);
  return reload;
}

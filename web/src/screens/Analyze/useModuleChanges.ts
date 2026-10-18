import { useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import type { ModuleData } from '../../interface/ModuleData';
import {
  withoutIds,
  withRenamedModule,
  withReplacedModule,
  withReplacedModules,
} from './moduleList';
import type { ProjectAnalysis } from './useProjectAnalysis';

/** What the page does once the API answered a change to the modules. */
export interface ModuleChanges {
  deleted: (moduleId: string) => void;
  renamed: (moduleId: string, newName: string) => void;
  split: (oldModuleId: string, halves: ModuleData[]) => void;
  merged: (sourceId: string, targetId: string, merged: ModuleData) => void;
  referencesChanged: (updated: ModuleData) => void;
  modeChanged: (updated: ModuleData) => void;
}

/**
 * Puts the new modules on screen (the graph follows them) and asks for the recommendations again.
 * The API re-runs the analyses after every change, so the panel would otherwise offer changes to
 * modules that no longer exist (#28). A new enforce mode changes no analysis, so it keeps them.
 */
export function useModuleChanges(analysis: ProjectAnalysis): ModuleChanges {
  const queryClient = useQueryClient();
  const { setModules } = analysis;
  return useMemo(() => {
    const changed = (update: (modules: ModuleData[]) => ModuleData[], rerated = true) => {
      setModules(update);
      if (rerated) void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    };
    return {
      deleted: (id) => changed((modules) => withoutIds(modules, [id])),
      renamed: (id, name) => changed((modules) => withRenamedModule(modules, id, name)),
      split: (oldId, halves) => changed((modules) => withReplacedModules(modules, [oldId], halves)),
      merged: (sourceId, targetId, merged) =>
        changed((modules) => withReplacedModules(modules, [sourceId, targetId], [merged])),
      referencesChanged: (updated) => changed((modules) => withReplacedModule(modules, updated)),
      modeChanged: (updated) => changed((modules) => withReplacedModule(modules, updated), false),
    };
  }, [queryClient, setModules]);
}

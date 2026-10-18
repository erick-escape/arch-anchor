import { useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import type { SidebarProps } from '../../components/Sidebar/sidebarTypes';
import type { ModuleData } from '../../interface/ModuleData';
import {
  withoutIds,
  withRenamedModule,
  withReplacedModule,
  withReplacedModules,
} from './moduleList';
import type { ProjectAnalysis } from './useProjectAnalysis';

export type ModuleChanges = Omit<SidebarProps, 'isOpen' | 'modules'> & {
  onMerged: (sourceId: string, targetId: string, merged: ModuleData) => void;
};

/**
 * What the page does after the API answered a change to the modules: put the new modules on
 * screen (the graph follows them) and ask for the recommendations again. The API re-runs the
 * analyses after every change, so the panel would otherwise offer changes to modules that no
 * longer exist (#28). A new enforce mode changes no analysis, so it keeps them.
 */
export function useModuleChanges(analysis: ProjectAnalysis): ModuleChanges {
  const queryClient = useQueryClient();
  const { setModules } = analysis;
  return useMemo(() => {
    const refresh = () => void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    const changed = (update: (modules: ModuleData[]) => ModuleData[], rerated = true) => {
      setModules(update);
      if (rerated) refresh();
    };
    return {
      onDeleteRefresh: (id) => changed((modules) => withoutIds(modules, [id])),
      onRenameRefresh: (id, name) => changed((modules) => withRenamedModule(modules, id, name)),
      onSplitRefresh: (oldId, halves) =>
        changed((modules) => withReplacedModules(modules, [oldId], halves)),
      onRefClazzModeRefresh: (_id, updated) =>
        changed((modules) => withReplacedModule(modules, updated), false),
      onRefClazzesRefresh: (_id, updated) =>
        changed((modules) => withReplacedModule(modules, updated)),
      onMerged: (sourceId, targetId, merged) =>
        changed((modules) => withReplacedModules(modules, [sourceId, targetId], [merged])),
    };
  }, [queryClient, setModules]);
}

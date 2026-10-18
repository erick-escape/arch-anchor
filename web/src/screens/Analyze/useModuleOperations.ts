import { useMemo } from 'react';
import { useToast, type Toast } from '../../components/feedback/useToast';
import type { EnforceMode } from '../../interface/ClazzData';
import type { ModuleData } from '../../interface/ModuleData';
import {
  deleteModule,
  mergeModules,
  renameModule,
  setEnforceMode,
  setReferenceClasses,
  splitModule,
} from '../../services/moduleApi';
import type { ModuleChanges } from './useModuleChanges';

/** Each operation resolves true once the API made the change, false after reporting a failure. */
export interface ModuleOperations {
  rename: (module: ModuleData, newName: string) => Promise<boolean>;
  remove: (module: ModuleData) => Promise<boolean>;
  split: (module: ModuleData, classIds: string[]) => Promise<boolean>;
  merge: (source: ModuleData, target: ModuleData) => Promise<boolean>;
  setReferences: (module: ModuleData, classIds: string[]) => Promise<boolean>;
  setMode: (module: ModuleData, classId: string, mode: EnforceMode) => Promise<boolean>;
}

// Asks the API for a change, puts its answer on screen, and says how it went. Every failure used
// to reach only the console. Arrow properties, so a method can be passed as a callback.
class ReportedModuleOperations implements ModuleOperations {
  constructor(
    private readonly changes: ModuleChanges,
    private readonly toast: Toast
  ) {}

  rename = (module: ModuleData, newName: string) =>
    this.reported(async () => {
      await renameModule(module.id, newName);
      this.changes.renamed(module.id, newName);
      return `Renamed ${module.name} to ${newName}`;
    });

  remove = (module: ModuleData) =>
    this.reported(async () => {
      await deleteModule(module.id);
      this.changes.deleted(module.id);
      return `Deleted ${module.name}`;
    });

  split = (module: ModuleData, classIds: string[]) =>
    this.reported(async () => {
      this.changes.split(module.id, await splitModule(module.id, classIds));
      return `Split ${module.name} in two`;
    });

  merge = (source: ModuleData, target: ModuleData) =>
    this.reported(async () => {
      this.changes.merged(source.id, target.id, await mergeModules(source.id, target.id));
      return `Merged ${source.name} into ${target.name}`;
    });

  setReferences = (module: ModuleData, classIds: string[]) =>
    this.reported(async () => {
      this.changes.referencesChanged(await setReferenceClasses(module.id, classIds));
      return `New reference ${classIds.length === 1 ? 'class' : 'classes'} for ${module.name}`;
    });

  setMode = (module: ModuleData, classId: string, mode: EnforceMode) =>
    this.reported(async () => {
      const { refClazzes, clazzes } = await setEnforceMode(module.id, classId, mode);
      this.changes.modeChanged({ ...module, refClazzes, clazzes });
      return `Exported as ${mode === 'MUST' ? 'MUST-DEPEND' : 'CAN-DEPEND'} from now on`;
    });

  private async reported(change: () => Promise<string>): Promise<boolean> {
    try {
      this.toast.success(await change());
      return true;
    } catch (error) {
      this.toast.error((error as Error).message);
      return false;
    }
  }
}

/**
 * The changes an architect makes to the modules by hand, from the modules panel or by dragging
 * one module onto another.
 *
 * @example const operations = useModuleOperations(changes); await operations.rename(web, 'ui');
 */
export function useModuleOperations(changes: ModuleChanges): ModuleOperations {
  const toast = useToast();
  return useMemo(() => new ReportedModuleOperations(changes, toast), [changes, toast]);
}

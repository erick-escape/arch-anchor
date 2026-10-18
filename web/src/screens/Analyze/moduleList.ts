import type { ModuleData } from '../../interface/ModuleData';

/**
 * The modules without the given ids.
 *
 * @example setModules((modules) => withoutIds(modules, [deletedId]));
 */
export function withoutIds<T extends { id: string }>(items: T[], ids: string[]): T[] {
  return items.filter((item) => !ids.includes(item.id));
}

/**
 * The modules with one renamed, leaving the previous array and module untouched.
 *
 * @example setModules((modules) => withRenamedModule(modules, 'm1', 'billing'));
 */
export function withRenamedModule(
  modules: ModuleData[],
  moduleId: string,
  newName: string
): ModuleData[] {
  return modules.map((module) => (module.id === moduleId ? { ...module, name: newName } : module));
}

/**
 * The modules with one replaced by the version the API returned after a change.
 *
 * @example setModules((modules) => withReplacedModule(modules, response.data));
 */
export function withReplacedModule(modules: ModuleData[], changed: ModuleData): ModuleData[] {
  return modules.map((module) => (module.id === changed.id ? changed : module));
}

/**
 * The modules after a merge: the two merged ones replaced by the merged module, or after a
 * split: the split one replaced by its halves.
 *
 * @example setModules((modules) => withReplacedModules(modules, [sourceId, targetId], [merged]));
 */
export function withReplacedModules(
  modules: ModuleData[],
  goneIds: string[],
  added: ModuleData[]
): ModuleData[] {
  return [...withoutIds(modules, goneIds), ...added];
}

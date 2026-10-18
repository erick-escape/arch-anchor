import axios from 'axios';
import type { EnforceMode } from '../interface/ClazzData';
import type { ModuleData, SplitModuleResponse } from '../interface/ModuleData';
import { problemDetailOf } from './problemDetail';

// Every call reports a failure as an Error written for the user: the API's own explanation when
// it sent one, otherwise what could not be done. They used to go to the console only.
async function attempt<T>(action: string, request: () => Promise<T>): Promise<T> {
  try {
    return await request();
  } catch (error) {
    throw new Error(
      problemDetailOf(error) ??
        `Could not ${action}. Check that the API is running, then try again.`
    );
  }
}

/** @example await renameModule('m1', 'checkout'); */
export function renameModule(moduleId: string, newName: string): Promise<void> {
  return attempt('rename the module', async () => {
    await axios.post('/api/module/rename', null, { params: { moduleId, newName } });
  });
}

/** @example await deleteModule('m1'); */
export function deleteModule(moduleId: string): Promise<void> {
  return attempt('delete the module', async () => {
    await axios.delete('/api/module/delete', { params: { moduleId } });
  });
}

/**
 * Moves the given classes out into a new module; the API answers with both halves.
 *
 * @example const halves = await splitModule('m1', ['c2']);
 */
export function splitModule(moduleId: string, classIds: string[]): Promise<ModuleData[]> {
  return attempt('split the module', async () => {
    const response = await axios.post<SplitModuleResponse>('/api/module/split', {
      moduleId,
      classIds,
    });
    return response.data.newModules;
  });
}

/**
 * Makes the given classes the module's reference classes, chosen by hand; the API answers with
 * the module recomputed around them.
 *
 * @example const module = await setReferenceClasses('m1', ['c2']);
 */
export function setReferenceClasses(moduleId: string, classIds: string[]): Promise<ModuleData> {
  return attempt('set the reference classes', async () => {
    const response = await axios.post<ModuleData>('/api/module/ref-clazzes', {
      moduleId,
      classIds,
    });
    return response.data;
  });
}

/** @example const module = await setEnforceMode('m1', 'c1', 'MUST'); */
export function setEnforceMode(
  moduleId: string,
  classId: string,
  mode: EnforceMode
): Promise<ModuleData> {
  return attempt('change the enforce mode', async () => {
    const response = await axios.post<ModuleData>('/api/module/ref-clazz-mode', {
      moduleId,
      classId,
      mode,
    });
    return response.data;
  });
}

/**
 * Merges the source module into the target; the API answers with the merged module, which keeps
 * the reference classes of both (#34).
 *
 * @example const merged = await mergeModules(web.id, persistence.id);
 */
export function mergeModules(sourceId: string, targetId: string): Promise<ModuleData> {
  return attempt('merge the modules', async () => {
    const response = await axios.post<ModuleData>('/api/module/merge', null, {
      params: { sourceId, targetId },
    });
    return response.data;
  });
}

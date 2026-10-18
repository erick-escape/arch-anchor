import axios from 'axios';
import type { ModuleData } from '../interface/ModuleData';
import { problemDetailOf } from './problemDetail';

/**
 * Merges the source module into the target; the API answers with the merged module, which keeps
 * the reference classes of both (#34).
 *
 * @example const merged = await mergeModules(web.id, persistence.id);
 */
export async function mergeModules(sourceId: string, targetId: string): Promise<ModuleData> {
  try {
    const response = await axios.post<ModuleData>('/api/module/merge', null, {
      params: { sourceId, targetId },
    });
    return response.data;
  } catch (error) {
    throw new Error(
      problemDetailOf(error) ??
        'Could not merge the modules. Check that the API is running, then try again.'
    );
  }
}

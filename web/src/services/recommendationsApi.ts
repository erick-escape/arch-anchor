import { api } from './api';
import { RecommendationsResponse } from '../interface/Recommendations';
import type { ApplyAction } from '../screens/Analyze/recommendations/recommendationCards';
import { problemDetailOf } from './problemDetail';

export const getRecommendations = async (): Promise<RecommendationsResponse> => {
  const response = await api.get<RecommendationsResponse>('/api/recommendations');
  return response.data;
};

export interface ApplySplitRequest {
  moduleId: string;
  classIds: string[];
}

export const applySplit = async (request: ApplySplitRequest): Promise<void> => {
  await api.post('/api/recommendations/apply/split', request);
};

export const applyMerge = async (sourceId: string, targetId: string): Promise<void> => {
  await api.post('/api/recommendations/apply/merge', null, {
    params: { sourceId, targetId },
  });
};

export const applyMove = async (
  classId: string,
  sourceModuleId: string,
  targetModuleId: string
): Promise<void> => {
  await api.post('/api/recommendations/apply/move', null, {
    params: { classId, sourceModuleId, targetModuleId },
  });
};

/**
 * Applies a recommendation as the split, merge or move it describes. A failure becomes an Error
 * written for the user; it used to surface axios's "Request failed with status code 500".
 *
 * @example await applyRecommendation({ kind: 'move', classId, sourceModuleId, targetModuleId });
 */
export async function applyRecommendation(action: ApplyAction): Promise<void> {
  try {
    if (action.kind === 'split')
      await applySplit({ moduleId: action.moduleId, classIds: action.classIds });
    if (action.kind === 'merge') await applyMerge(action.sourceId, action.targetId);
    if (action.kind === 'move')
      await applyMove(action.classId, action.sourceModuleId, action.targetModuleId);
  } catch (error) {
    throw new Error(
      problemDetailOf(error) ??
        `Could not apply the ${action.kind}. Check that the API is running, then try again.`
    );
  }
}

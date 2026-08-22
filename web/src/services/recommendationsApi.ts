import { api } from './api';
import { RecommendationsResponse } from '../interface/Recommendations';

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

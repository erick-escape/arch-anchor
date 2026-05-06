import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { message } from 'antd';
import {
  getRecommendations,
  applySplit,
  applyMerge,
  applyMove,
  ApplySplitRequest
} from '../services/recommendationsApi';

export const useRecommendations = () => {
  return useQuery({
    queryKey: ['recommendations'],
    queryFn: getRecommendations,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

export const useApplySplit = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: ApplySplitRequest) => applySplit(request),
    onSuccess: () => {
      message.success('Split module recommendation applied successfully');
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['project'] });
    },
    onError: (error: any) => {
      message.error(`Failed to apply split: ${error.message || 'Unknown error'}`);
    },
  });
};

export const useApplyMerge = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sourceId, targetId }: { sourceId: string; targetId: string }) =>
      applyMerge(sourceId, targetId),
    onSuccess: () => {
      message.success('Merge module recommendation applied successfully');
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['project'] });
    },
    onError: (error: any) => {
      message.error(`Failed to apply merge: ${error.message || 'Unknown error'}`);
    },
  });
};

export const useApplyMove = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      classId,
      sourceModuleId,
      targetModuleId,
    }: {
      classId: string;
      sourceModuleId: string;
      targetModuleId: string;
    }) => applyMove(classId, sourceModuleId, targetModuleId),
    onSuccess: () => {
      message.success('Move class recommendation applied successfully');
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['project'] });
    },
    onError: (error: any) => {
      message.error(`Failed to apply move: ${error.message || 'Unknown error'}`);
    },
  });
};

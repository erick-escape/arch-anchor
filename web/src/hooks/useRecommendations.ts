import { useQuery } from '@tanstack/react-query';
import { getRecommendations } from '../services/recommendationsApi';

/**
 * The recommendations of one project. The API answers for the project analysed last, so the
 * query waits until this project's analysis has returned, and is keyed by the project: opening
 * a project no longer shows the previous one's recommendations first (#53).
 *
 * @example const { data } = useRecommendations('bookstore', analysis.status === 'ready');
 */
export const useRecommendations = (projectName: string, analyzed: boolean) => {
  return useQuery({
    queryKey: ['recommendations', projectName],
    queryFn: getRecommendations,
    enabled: analyzed,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

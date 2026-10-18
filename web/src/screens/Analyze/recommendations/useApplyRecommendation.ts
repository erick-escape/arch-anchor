import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '../../../components/feedback/useToast';
import { applyRecommendation } from '../../../services/recommendationsApi';
import { useGraphEmphasisActions } from '../graphEmphasisContext';
import type { Applicable, RecommendationCardModel } from './recommendationCards';

export interface CardToApply {
  card: RecommendationCardModel;
  applicable: Applicable;
}

/**
 * Applies a confirmed recommendation: the API makes the change, the change lands on the graph (a
 * moved class's tile travels to its new module), the page shows the saved modules and the
 * recommendations are asked for again.
 *
 * @example const apply = useApplyRecommendation(reloadSavedModules); apply.mutate({ card, applicable });
 */
export function useApplyRecommendation(onApplied: () => void) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { land, highlight } = useGraphEmphasisActions();
  return useMutation({
    mutationFn: ({ applicable }: CardToApply) => applyRecommendation(applicable.action),
    onSuccess: (_, { card, applicable }) => {
      toast.success(applicable.done);
      highlight([]);
      const { action } = applicable;
      land(
        card.touched,
        action.kind === 'move'
          ? { from: action.sourceModuleId, to: action.targetModuleId }
          : undefined
      );
      void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      onApplied();
    },
    onError: (error: Error) => toast.error(error.message),
  });
}

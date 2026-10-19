import { useReactFlow } from '@xyflow/react';
import { useCallback, useState } from 'react';
import { useGraphEmphasisActions } from './graphEmphasisContext';
import { cardDomIdOf } from './recommendations/cardDomId';
import type { RecommendationCardModel } from './recommendations/recommendationCards';
import type { RecommendationsTab } from './recommendations/RecommendationsPanel';
import type { PanelLayout } from './usePanelLayout';

export interface AnalyzeTargets {
  shownModuleId: string | null;
  setShownModuleId: (moduleId: string | null) => void;
  tab: RecommendationsTab;
  setTab: (tab: RecommendationsTab) => void;
  /** Opens a module in the modules panel and brings it into view on the graph. */
  showModule: (moduleId: string) => void;
  /** Opens a card's tab, highlights what it touches, and focuses the card. */
  showRecommendation: (card: RecommendationCardModel) => void;
}

// After the tab has switched and the card is drawn; a card past the first page is not.
function focusCardSoon(cardId: string): void {
  setTimeout(() => {
    const card = document.getElementById(cardDomIdOf(cardId));
    card?.scrollIntoView?.({ block: 'nearest' });
    card?.focus({ preventScroll: true });
  });
}

/**
 * What the Analyze page shows in its panels, and the moves to a module or a recommendation the
 * command palette makes.
 *
 * @example const targets = useAnalyzeTargets(layout); targets.showModule('m3');
 */
export function useAnalyzeTargets(layout: PanelLayout): AnalyzeTargets {
  const [shownModuleId, setShownModuleId] = useState<string | null>(null);
  const [tab, setTab] = useState<RecommendationsTab>('refactorings');
  const { fitView } = useReactFlow();
  const { highlight } = useGraphEmphasisActions();
  const { show } = layout;
  const showModule = useCallback(
    (moduleId: string) => {
      show('modules');
      setShownModuleId(moduleId);
      void fitView({ nodes: [{ id: moduleId }], duration: 400, maxZoom: 1.2, padding: 0.6 });
    },
    [show, fitView]
  );
  const showRecommendation = useCallback(
    (card: RecommendationCardModel) => {
      show('recommendations');
      setTab(card.rate === null ? 'violations' : 'refactorings');
      highlight(card.touched);
      focusCardSoon(card.id);
    },
    [show, highlight]
  );
  return { shownModuleId, setShownModuleId, tab, setTab, showModule, showRecommendation };
}

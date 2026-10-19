import { useMemo } from 'react';
import { useRegisterCommands } from '../../components/CommandPalette/paletteContext';
import { useRecommendations } from '../../hooks/useRecommendations';
import type { ModuleData } from '../../interface/ModuleData';
import {
  actionCommandsOf,
  classCommandsOf,
  moduleCommandsOf,
  recommendationCommandsOf,
  type PageActions,
} from './analyzeCommands';
import { refactoringCardsOf, violationCardsOf } from './recommendations/recommendationCards';
import type { AnalyzeTargets } from './useAnalyzeTargets';

interface AnalyzeCommandSources {
  projectName: string;
  modules: ModuleData[];
  targets: AnalyzeTargets;
  actions: PageActions;
}

/**
 * Offers the analysis in the command palette: its modules, classes and recommendations, and
 * the page's actions, while the page is open.
 *
 * @example useAnalyzeCommands({ projectName, modules, targets, actions });
 */
export function useAnalyzeCommands({
  projectName,
  modules,
  targets,
  actions,
}: AnalyzeCommandSources): void {
  const recommendations = useRecommendations(projectName, true);
  const { showModule, showRecommendation } = targets;
  const commands = useMemo(() => {
    const response = recommendations.data;
    const cards = response ? [...refactoringCardsOf(response), ...violationCardsOf(response)] : [];
    return [
      ...moduleCommandsOf(modules, showModule),
      ...classCommandsOf(modules, showModule),
      ...recommendationCommandsOf(cards, showRecommendation),
      ...actionCommandsOf(actions),
    ];
  }, [modules, recommendations.data, showModule, showRecommendation, actions]);
  useRegisterCommands('analyze', commands);
}

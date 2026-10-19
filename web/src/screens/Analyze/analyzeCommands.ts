import type { PaletteCommand } from '../../components/CommandPalette/commandSearch';
import type { ModuleData } from '../../interface/ModuleData';
import { formatPercent, formatRate } from '../../utils/formatPercent';
import { priorityOf } from '../../utils/priority';
import type { RecommendationCardModel } from './recommendations/recommendationCards';
import type { PanelName } from './usePanelLayout';

/**
 * The modules, for the palette: each opens in the modules panel and on the graph.
 *
 * @example moduleCommandsOf(modules, showModule)[0].label // 'bookstore/model'
 */
export function moduleCommandsOf(
  modules: ModuleData[],
  showModule: (id: string) => void
): PaletteCommand[] {
  return modules.map((module) => ({
    id: `module:${module.id}`,
    group: 'Modules',
    label: module.name,
    detail: `${module.clazzes.length} classes · ${formatPercent(module.similarity)}`,
    run: () => showModule(module.id),
  }));
}

/**
 * Every class, for the palette: each opens its module.
 *
 * @example classCommandsOf(modules, showModule) // [{ label: 'SalesReport', detail: 'bookstore/web · 50.00%' }, …]
 */
export function classCommandsOf(
  modules: ModuleData[],
  showModule: (id: string) => void
): PaletteCommand[] {
  return modules.flatMap((module) =>
    module.clazzes.map((clazz) => ({
      id: `class:${module.id}:${clazz.id}`,
      group: 'Classes' as const,
      label: clazz.name,
      detail: `${module.name} · ${formatPercent(clazz.similarity)}`,
      run: () => showModule(module.id),
    }))
  );
}

/**
 * The recommendations, for the palette: each opens its card in the recommendations panel.
 *
 * @example recommendationCommandsOf(cards, showCard)[0].detail // 'High priority · 0.63'
 */
export function recommendationCommandsOf(
  cards: RecommendationCardModel[],
  showRecommendation: (card: RecommendationCardModel) => void
): PaletteCommand[] {
  return cards.map((card) => ({
    id: `recommendation:${card.id}`,
    group: 'Recommendations',
    label: card.title,
    detail:
      card.rate === null
        ? 'Violation'
        : `${priorityOf(card.rate)} priority · ${formatRate(card.rate)}`,
    run: () => showRecommendation(card),
  }));
}

export interface PageActions {
  exportAcs: () => void;
  openSettings: () => void;
  toggle: (panel: PanelName) => void;
  open: Record<PanelName, boolean>;
}

/**
 * The Analyze page's own actions, for the palette. A panel toggle says what it will do.
 *
 * @example actionCommandsOf(actions).map((command) => command.label) // ['Export ACs', 'Settings', …]
 */
export function actionCommandsOf(actions: PageActions): PaletteCommand[] {
  const toggle = (panel: PanelName, name: string): PaletteCommand => ({
    id: `panel:${panel}`,
    group: 'Actions',
    label: `${actions.open[panel] ? 'Hide' : 'Show'} the ${name} panel`,
    run: () => actions.toggle(panel),
  });
  return [
    {
      id: 'action:export',
      group: 'Actions',
      label: 'Export ACs',
      detail: 'PDF',
      run: actions.exportAcs,
    },
    {
      id: 'action:settings',
      group: 'Actions',
      label: 'Settings',
      detail: 'Rating weights and analysis filters',
      run: actions.openSettings,
    },
    toggle('modules', 'modules'),
    toggle('recommendations', 'recommendations'),
  ];
}

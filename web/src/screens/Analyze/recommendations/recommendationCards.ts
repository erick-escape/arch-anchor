import type {
  ArchitectureViolationRecommendation,
  MergeModuleRecommendation,
  MoveClassRecommendation,
  RecommendationsResponse,
  RecommendationType,
  SplitModuleRecommendation,
} from '../../../interface/Recommendations';
import { formatPercent, formatRate } from '../../../utils/formatPercent';
import { formatSignedPercent } from '../../../utils/formatSignedPercent';

/** What Apply sends: the API applies a recommendation as a split, a merge or a move. */
export type ApplyAction =
  | { kind: 'split'; moduleId: string; classIds: string[] }
  | { kind: 'merge'; sourceId: string; targetId: string }
  | { kind: 'move'; classId: string; sourceModuleId: string; targetModuleId: string };

export interface Applicable {
  action: ApplyAction;
  /** The card's button, e.g. "Apply move…". */
  label: string;
  confirmTitle: string;
  confirmBody: string;
  /** The confirmation's button, e.g. "Apply move". */
  confirmLabel: string;
  /** What the toast says once applied. */
  done: string;
}

export interface DetailRow {
  label: string;
  value: string;
}

export interface RecommendationCardModel {
  id: string;
  type: RecommendationType;
  /** The rate of a split, merge or move; null for a violation, which has none (#48). */
  rate: number | null;
  title: string;
  description: string;
  details: DetailRow[];
  /** Similarity before and after, drawn as two meters. */
  similarity?: { before: number; after: number; of: string };
  violatingTypes?: string[];
  /** The graph's nodes the change touches: highlighted while the card is hovered. */
  touched: string[];
  apply: Applicable | { unavailable: string };
}

function transition(before: number, after: number): string {
  return `${before} → ${after}`;
}

function splitCard(split: SplitModuleRecommendation, index: number): RecommendationCardModel {
  const { metrics } = split;
  const moved = split.module2ClassIds.length;
  return {
    id: `split-${index}`,
    type: 'split',
    rate: metrics.rate,
    title: `Split “${split.originalModuleName}”`,
    description: `${split.module1ClassIds.length} classes stay; ${moved} ${moved === 1 ? 'moves' : 'move'} to a new module.`,
    details: [
      {
        label: 'Violations',
        value: transition(metrics.originalViolations, metrics.totalSplitViolations),
      },
      { label: 'Halves', value: `${split.module1ClassIds.length} and ${moved} classes` },
      { label: 'Reference classes', value: `${split.superRefClassName} · ${split.deRefClassName}` },
    ],
    similarity: {
      before: metrics.originalSimilarity,
      after: metrics.avgSplitSimilarity,
      of: split.originalModuleName,
    },
    touched: [split.originalModuleId],
    apply: splitApplicable(split),
  };
}

function splitApplicable(split: SplitModuleRecommendation): Applicable {
  const { metrics } = split;
  return {
    action: { kind: 'split', moduleId: split.originalModuleId, classIds: split.module2ClassIds },
    label: 'Apply split…',
    confirmTitle: `Split ${split.originalModuleName} in two?`,
    confirmBody: `Rated ${formatRate(metrics.rate)}. ${split.superRefClassName}'s half keeps the module; ${split.deRefClassName}'s becomes a new one.`,
    confirmLabel: 'Apply split',
    done: `Split ${split.originalModuleName} in two`,
  };
}

function mergeCard(merge: MergeModuleRecommendation, index: number): RecommendationCardModel {
  const { metrics } = merge;
  const violations = `(${metrics.module1Violations} + ${metrics.module2Violations}) → ${metrics.mergedViolations}`;
  return {
    id: `merge-${index}`,
    type: 'merge',
    rate: metrics.rate,
    title: `Merge “${merge.module1Name}” and “${merge.module2Name}”`,
    description: 'One module with the reference classes of both.',
    details: [
      {
        label: 'Module similarities',
        value: `${formatPercent(metrics.module1Similarity)} · ${formatPercent(metrics.module2Similarity)}`,
      },
      { label: 'Merged similarity', value: formatPercent(metrics.mergedSimilarity) },
      { label: 'Similarity change', value: formatSignedPercent(metrics.similarityImprovement) },
      { label: 'Violations', value: violations },
    ],
    touched: [merge.module1Id, merge.module2Id],
    apply: mergeApplicable(merge),
  };
}

function mergeApplicable(merge: MergeModuleRecommendation): Applicable {
  const change = formatSignedPercent(merge.metrics.similarityImprovement);
  return {
    action: { kind: 'merge', sourceId: merge.module1Id, targetId: merge.module2Id },
    label: 'Apply merge…',
    confirmTitle: `Merge ${merge.module1Name} and ${merge.module2Name}?`,
    confirmBody: `Rated ${formatRate(merge.metrics.rate)}. Similarity changes by ${change}; the merged module keeps the reference classes of both.`,
    confirmLabel: 'Apply merge',
    done: `Merged ${merge.module1Name} and ${merge.module2Name}`,
  };
}

function moveCard(move: MoveClassRecommendation, index: number): RecommendationCardModel {
  const { metrics } = move;
  return {
    id: `move-${index}`,
    type: 'move',
    rate: metrics.rate,
    title: `Move “${move.className}”`,
    description: `From ${move.sourceModuleName} to ${move.targetModuleName}.`,
    details: [
      { label: 'Similarity change', value: formatSignedPercent(metrics.similarityImprovement) },
      {
        label: 'Source violations',
        value: transition(metrics.sourceViolationsBefore, metrics.sourceViolationsAfter),
      },
      {
        label: 'Target violations',
        value: transition(metrics.targetViolationsBefore, metrics.targetViolationsAfter),
      },
    ],
    touched: [move.sourceModuleId, move.targetModuleId],
    apply: moveApplicable(
      move.classId,
      move.className,
      move.sourceModuleId,
      move.targetModuleId,
      move.targetModuleName,
      `Rated ${formatRate(metrics.rate)}. ${move.sourceModuleName} goes from ${metrics.sourceViolationsBefore} to ${metrics.sourceViolationsAfter} violations.`
    ),
  };
}

function moveApplicable(
  classId: string,
  className: string,
  sourceModuleId: string,
  targetModuleId: string,
  targetModuleName: string,
  confirmBody: string
): Applicable {
  return {
    action: { kind: 'move', classId, sourceModuleId, targetModuleId },
    label: 'Apply move…',
    confirmTitle: `Move ${className} to ${targetModuleName}?`,
    confirmBody,
    confirmLabel: 'Apply move',
    done: `Moved ${className} to ${targetModuleName}`,
  };
}

function violationCard(
  violation: ArchitectureViolationRecommendation,
  index: number
): RecommendationCardModel {
  const suggestion = violation.bestSuggestion;
  return {
    id: `violation-${index}`,
    type: 'violation',
    rate: null,
    title: `Fix violation in “${violation.className}”`,
    description: `${violation.sourceModuleName} is not allowed to use ${violation.violation}.`,
    details: suggestionDetails(violation),
    violatingTypes: violation.violatingDependencies,
    touched: [
      violation.sourceModuleId,
      ...(suggestion ? [suggestion.targetModuleId] : []),
      `package:${violation.violation}`,
    ],
    apply: violationApplicable(violation),
  };
}

function suggestionDetails(violation: ArchitectureViolationRecommendation): DetailRow[] {
  const suggestion = violation.bestSuggestion;
  if (!suggestion) return [];
  return [
    { label: 'Suggested target', value: suggestion.targetModuleName },
    {
      label: 'Similarity improvement',
      value: formatSignedPercent(suggestion.similarityImprovement),
    },
    { label: 'New violations', value: String(suggestion.newViolationsCreated) },
  ];
}

function violationApplicable(
  violation: ArchitectureViolationRecommendation
): RecommendationCardModel['apply'] {
  const suggestion = violation.bestSuggestion;
  if (!suggestion || suggestion.requiresNewModule) {
    return {
      unavailable: `No existing module fits ${violation.className}: it needs a module of its own. Split ${violation.sourceModuleName} instead.`,
    };
  }
  return moveApplicable(
    violation.classId,
    violation.className,
    violation.sourceModuleId,
    suggestion.targetModuleId,
    suggestion.targetModuleName,
    `${violation.sourceModuleName} loses its ${violation.violation} violation; ${suggestion.newViolationsCreated} new ones.`
  );
}

/**
 * The Refactorings tab: splits, merges and moves appended in that order, then sorted by rate,
 * highest first. The sort is stable, so equal rates keep that order. The paper's evaluation
 * harness (ICSE/evaluation/harness/panel.py) copies this order: keep it.
 *
 * @example refactoringCardsOf(response)[0].title // 'Split “bookstore/web”'
 */
export function refactoringCardsOf(response: RecommendationsResponse): RecommendationCardModel[] {
  const cards = [
    ...response.splits.map(splitCard),
    ...response.merges.map(mergeCard),
    ...response.moves.map(moveCard),
  ];
  return cards.sort((a, b) => (b.rate ?? 0) - (a.rate ?? 0));
}

/** The Violations tab, in the API's order. */
export function violationCardsOf(response: RecommendationsResponse): RecommendationCardModel[] {
  return response.violations.map(violationCard);
}

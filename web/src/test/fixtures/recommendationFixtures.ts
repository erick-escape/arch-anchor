import {
  ArchitectureViolationRecommendation,
  MergeModuleRecommendation,
  MoveClassRecommendation,
  RecommendationsResponse,
  SplitModuleRecommendation,
  UnifiedRecommendation,
} from '../../interface/Recommendations';

type RecommendationLists = Omit<RecommendationsResponse, 'summary'>;

/**
 * Builds the body of `GET /api/recommendations` from the given lists, with a matching summary.
 *
 * @example
 * fakeApi.respondTo('GET', '/api/recommendations', recommendationsFixture({ moves: [move] }));
 */
export function recommendationsFixture(
  overrides: Partial<RecommendationLists> = {}
): RecommendationsResponse {
  const lists: RecommendationLists = {
    splits: [],
    merges: [],
    moves: [],
    violations: [],
    ...overrides,
  };
  const total =
    lists.splits.length + lists.merges.length + lists.moves.length + lists.violations.length;
  return {
    ...lists,
    summary: {
      totalRecommendations: total,
      highPriority: total,
      mediumPriority: 0,
      lowPriority: 0,
    },
  };
}

/**
 * Builds a move recommendation as the backend serialises it (`MoveClassRecommendationDTO`).
 *
 * @example
 * const move = moveRecommendationFixture({ className: 'SalesReport' });
 */
export function moveRecommendationFixture(
  overrides: Partial<MoveClassRecommendation> = {}
): MoveClassRecommendation {
  return {
    classId: 'c9',
    className: 'SalesReport',
    sourceModuleId: 'm1',
    sourceModuleName: 'shop/web',
    targetModuleId: 'm2',
    targetModuleName: 'shop/persistence',
    metrics: {
      classCurrentSimilarity: 0,
      classTargetSimilarity: 0,
      sourceViolationsBefore: 1,
      sourceViolationsAfter: 0,
      targetViolationsBefore: 0,
      targetViolationsAfter: 0,
      similarityImprovement: 0.125,
      violationsImprovement: 1,
      rate: 0.5625,
    },
    ...overrides,
  };
}

/**
 * Builds the card the recommendations panel draws for a move of the class `className`.
 *
 * @example
 * const cards = [moveCardFixture('Report0'), moveCardFixture('Report1')];
 */
export function moveCardFixture(className = 'SalesReport'): UnifiedRecommendation {
  const move = moveRecommendationFixture({ className });
  return {
    id: `move-${className}`,
    type: 'move',
    rating: move.metrics.rate,
    title: `Move "${className}"`,
    description: `Move class with ${move.metrics.rate.toFixed(2)} improvement`,
    data: move,
  };
}

/**
 * Builds a split recommendation as the backend serialises it (`SplitModuleRecommendationDTO`):
 * module 1 keeps the reference class, module 2 holds the classes to split off.
 *
 * @example
 * const split = splitRecommendationFixture({ module2ClassIds: ['c9'] });
 */
export function splitRecommendationFixture(
  overrides: Partial<SplitModuleRecommendation> = {}
): SplitModuleRecommendation {
  return {
    originalModuleId: 'm1',
    originalModuleName: 'shop/web',
    superRefClassName: 'CatalogController',
    deRefClassName: 'SalesReport',
    module1ClassIds: ['c1', 'c2', 'c3'],
    module2ClassIds: ['c9'],
    metrics: {
      originalSimilarity: 0.75,
      avgSplitSimilarity: 1,
      originalViolations: 1,
      totalSplitViolations: 0,
      similarityImprovement: 0.25,
      violationsImprovement: 1,
      rate: 0.625,
    },
    ...overrides,
  };
}

/**
 * Builds a merge recommendation as the backend serialises it (`MergeModuleRecommendationDTO`),
 * with the bookstore's numbers: merging costs similarity but removes the violation (#26).
 *
 * @example
 * const merge = mergeRecommendationFixture();
 */
export function mergeRecommendationFixture(
  overrides: Partial<MergeModuleRecommendation> = {}
): MergeModuleRecommendation {
  return {
    module1Id: 'm2',
    module1Name: 'bookstore/persistence',
    module2Id: 'm1',
    module2Name: 'bookstore/web',
    metrics: {
      module1Similarity: 1,
      module2Similarity: 0.75,
      mergedSimilarity: 0.7143,
      module1Violations: 0,
      module2Violations: 1,
      mergedViolations: 0,
      similarityImprovement: -0.1607,
      violationsImprovement: 1,
      rate: 0.4196,
    },
    ...overrides,
  };
}

/**
 * Builds a violation entry as the backend serialises it (`ArchitectureViolationRecommendationDTO`):
 * the bookstore's SalesReport, a web class that uses java.sql.
 *
 * @example
 * const violation = violationRecommendationFixture();
 */
export function violationRecommendationFixture(
  overrides: Partial<ArchitectureViolationRecommendation> = {}
): ArchitectureViolationRecommendation {
  return {
    classId: 'c9',
    className: 'SalesReport',
    sourceModuleId: 'm1',
    sourceModuleName: 'shop/web',
    violation: 'java.sql',
    violatingDependencies: ['java.sql.Connection'],
    bestSuggestion: {
      targetModuleId: 'm2',
      targetModuleName: 'shop/persistence',
      similarityImprovement: 0.25,
      newViolationsCreated: 0,
      requiresNewModule: false,
      reason: 'Perfect architectural fit, improved similarity and no new violations',
    },
    alternativeSuggestions: [],
    ...overrides,
  };
}

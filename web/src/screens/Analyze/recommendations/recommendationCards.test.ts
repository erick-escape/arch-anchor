import { describe, expect, it } from 'vitest';
import {
  mergeRecommendationFixture,
  moveRecommendationFixture,
  recommendationsFixture,
  splitRecommendationFixture,
  violationRecommendationFixture,
} from '../../../test/fixtures/recommendationFixtures';
import { refactoringCardsOf, violationCardsOf } from './recommendationCards';

describe('refactoringCardsOf', () => {
  // ICSE/evaluation/harness/panel.py copies this order to rank the cards of the paper's study:
  // splits, merges and moves appended in that order, then a stable sort by rate, highest first.
  it('keeps the panel’s order: by rate, highest first, ties in split, merge, move order', () => {
    const response = recommendationsFixture({
      moves: [
        moveRecommendationFixture({
          className: 'MoveA',
          metrics: { ...moveRecommendationFixture().metrics, rate: 0.5 },
        }),
      ],
      splits: [
        splitRecommendationFixture({
          originalModuleName: 'SplitA',
          metrics: { ...splitRecommendationFixture().metrics, rate: 0.5 },
        }),
      ],
      merges: [
        mergeRecommendationFixture({
          module1Name: 'MergeA',
          metrics: { ...mergeRecommendationFixture().metrics, rate: 0.9 },
        }),
      ],
    });

    expect(refactoringCardsOf(response).map((card) => card.type)).toEqual([
      'merge',
      'split',
      'move',
    ]);
  });

  it('writes what each change does and what Apply would send', () => {
    const [split, move] = refactoringCardsOf(
      recommendationsFixture({
        splits: [splitRecommendationFixture()],
        moves: [moveRecommendationFixture()],
      })
    );

    expect(split.title).toBe('Split “shop/web”');
    expect(split.description).toBe('3 classes stay; 1 moves to a new module.');
    expect(split.apply).toMatchObject({
      action: { kind: 'split', moduleId: 'm1', classIds: ['c9'] },
      label: 'Apply split…',
    });
    expect(move.title).toBe('Move “SalesReport”');
    expect(move.description).toBe('From shop/web to shop/persistence.');
    expect(move.apply).toMatchObject({
      action: { kind: 'move', classId: 'c9', sourceModuleId: 'm1', targetModuleId: 'm2' },
      confirmTitle: 'Move SalesReport to shop/persistence?',
    });
  });

  it('lists the details as label and value, signed changes with a typographic minus', () => {
    const [merge] = refactoringCardsOf(
      recommendationsFixture({ merges: [mergeRecommendationFixture()] })
    );

    expect(merge.details).toContainEqual({ label: 'Similarity change', value: '−16.1%' });
    expect(merge.details).toContainEqual({ label: 'Violations', value: '(0 + 1) → 0' });
  });

  it('names the modules a change touches, for the graph to highlight', () => {
    const cards = refactoringCardsOf(
      recommendationsFixture({
        merges: [mergeRecommendationFixture()],
        moves: [moveRecommendationFixture()],
      })
    );

    expect(cards.map((card) => card.touched)).toEqual([
      ['m1', 'm2'],
      ['m2', 'm1'],
    ]);
  });
});

describe('violationCardsOf', () => {
  it('names the forbidden package and its types, and applies the suggested move', () => {
    const [violation] = violationCardsOf(
      recommendationsFixture({ violations: [violationRecommendationFixture()] })
    );

    expect(violation.title).toBe('Fix violation in “SalesReport”');
    expect(violation.description).toBe('shop/web is not allowed to use java.sql.');
    expect(violation.violatingTypes).toEqual(['java.sql.Connection']);
    expect(violation.touched).toEqual(['m1', 'm2', 'package:java.sql']);
    expect(violation.apply).toMatchObject({
      action: { kind: 'move', targetModuleId: 'm2' },
      label: 'Apply move…',
    });
  });

  it('says why nothing can be applied when no module fits', () => {
    const [violation] = violationCardsOf(
      recommendationsFixture({
        violations: [
          violationRecommendationFixture({
            bestSuggestion: {
              ...violationRecommendationFixture().bestSuggestion!,
              requiresNewModule: true,
            },
          }),
        ],
      })
    );

    expect(violation.apply).toEqual({
      unavailable:
        'No existing module fits SalesReport: it needs a module of its own. Split shop/web instead.',
    });
  });

  it('has no rate, so no priority', () => {
    const [violation] = violationCardsOf(
      recommendationsFixture({ violations: [violationRecommendationFixture()] })
    );

    expect(violation.rate).toBeNull();
  });
});

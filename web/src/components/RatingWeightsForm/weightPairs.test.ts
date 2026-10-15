import { describe, expect, it } from 'vitest';
import { describeWeightPair, pairFromSimilarityWeight } from './weightPairs';

describe('pairFromSimilarityWeight', () => {
  it('gives violations the rest of 1', () => {
    expect(pairFromSimilarityWeight(0.25)).toEqual({ similarity: 0.25, violation: 0.75 });
  });

  // 1 - 0.7 is 0.30000000000000004 in floating point.
  it('rounds the violation weight to the precision of the slider', () => {
    expect(pairFromSimilarityWeight(0.7)).toEqual({ similarity: 0.7, violation: 0.3 });
  });

  it('accepts the ends of the range', () => {
    expect(pairFromSimilarityWeight(0)).toEqual({ similarity: 0, violation: 1 });
    expect(pairFromSimilarityWeight(1)).toEqual({ similarity: 1, violation: 0 });
  });
});

describe('describeWeightPair', () => {
  it('names both weights with two decimals', () => {
    expect(describeWeightPair({ similarity: 0.6, violation: 0.4 })).toBe(
      'Similarity 0.60 · Violations 0.40'
    );
  });
});

import { WeightPair } from '../../interface/RatingWeights';

// The sliders move in steps of 0.05, so two decimals hold every value they produce.
const roundToSliderPrecision = (weight: number): number => Math.round(weight * 100) / 100;

/**
 * The pair a slider at `similarity` stands for: violations get the rest of 1, so the pair
 * always sums to 1 as the API requires. Rounded, because 1 - 0.7 is 0.30000000000000004.
 *
 * @example
 * pairFromSimilarityWeight(0.7); // { similarity: 0.7, violation: 0.3 }
 */
export function pairFromSimilarityWeight(similarity: number): WeightPair {
  return {
    similarity: roundToSliderPrecision(similarity),
    violation: roundToSliderPrecision(1 - similarity),
  };
}

/**
 * @example
 * describeWeightPair({ similarity: 0.6, violation: 0.4 }); // 'Similarity 0.60 · Violations 0.40'
 */
export function describeWeightPair(pair: WeightPair): string {
  return `Similarity ${pair.similarity.toFixed(2)} · Violations ${pair.violation.toFixed(2)}`;
}

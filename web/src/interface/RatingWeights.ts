// Mirrors RatingWeightsDTO in api/. Each pair is non-negative and sums to 1.

export interface WeightPair {
  similarity: number;
  violation: number;
}

// The election picks each module's reference class; the recommendation rate ranks refactorings.
export interface RatingWeights {
  election: WeightPair;
  recommendation: WeightPair;
}

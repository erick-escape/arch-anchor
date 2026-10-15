// Mirrors AnalysisSettingsDTO in api/ (#36). The defaults filter nothing.
export interface AnalysisSettings {
  // The fewest classes a split may leave in either half.
  minSplitModuleClasses: number;
  // The fewest classes a move may leave in its source module.
  minSourceClassesAfterMove: number;
  // How many of a class's best-rated move targets are recommended; null for no limit.
  maxMoveTargetsPerClass: number | null;
  // The fewest package dependencies a class needs before it gets move or violation entries.
  minClassDependencies: number;
  // The lowest rate of a shown split, merge or move; violations are always shown.
  minRecommendationRate: number;
  // The lowest similarity change of a recommended merge (#41); 0 keeps out merges that lower
  // similarity, null for no limit.
  minMergeSimilarityChange: number | null;
}

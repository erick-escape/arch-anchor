// Metrics DTOs
export interface SplitMetrics {
  originalSimilarity: number;
  avgSplitSimilarity: number;
  originalViolations: number;
  totalSplitViolations: number;
  similarityImprovement: number;
  violationsImprovement: number;
  rate: number;
}

export interface MergeMetrics {
  module1Similarity: number;
  module2Similarity: number;
  mergedSimilarity: number;
  module1Violations: number;
  module2Violations: number;
  mergedViolations: number;
  similarityImprovement: number;
  violationsImprovement: number;
  rate: number;
}

export interface MoveMetrics {
  classCurrentSimilarity: number;
  classTargetSimilarity: number;
  sourceViolationsBefore: number;
  sourceViolationsAfter: number;
  targetViolationsBefore: number;
  targetViolationsAfter: number;
  similarityImprovement: number;
  violationsImprovement: number;
  rate: number;
}

export interface MoveSuggestion {
  targetModuleId: string;
  targetModuleName: string;
  similarityImprovement: number;
  newViolationsCreated: number;
  requiresNewModule: boolean;
  reason: string;
}

// Recommendation DTOs
export interface SplitModuleRecommendation {
  originalModuleId: string;
  originalModuleName: string;
  superRefClassName: string;
  deRefClassName: string;
  module1ClassIds: string[];
  module2ClassIds: string[];
  metrics: SplitMetrics;
}

export interface MergeModuleRecommendation {
  module1Id: string;
  module1Name: string;
  module2Id: string;
  module2Name: string;
  metrics: MergeMetrics;
}

export interface MoveClassRecommendation {
  classId: string;
  className: string;
  sourceModuleId: string;
  sourceModuleName: string;
  targetModuleId: string;
  targetModuleName: string;
  metrics: MoveMetrics;
}

export interface ArchitectureViolationRecommendation {
  classId: string;
  className: string;
  sourceModuleId: string;
  sourceModuleName: string;
  violation: string;
  violatingDependencies: string[];
  bestSuggestion: MoveSuggestion | null;
  alternativeSuggestions: MoveSuggestion[];
}

// Summary DTO
export interface RecommendationsSummary {
  totalRecommendations: number;
  highPriority: number;
  mediumPriority: number;
  lowPriority: number;
}

// Response DTO
export interface RecommendationsResponse {
  splits: SplitModuleRecommendation[];
  merges: MergeModuleRecommendation[];
  moves: MoveClassRecommendation[];
  violations: ArchitectureViolationRecommendation[];
  summary: RecommendationsSummary;
}

// Unified recommendation interface for display
export type RecommendationType = 'split' | 'merge' | 'move' | 'violation';

export interface UnifiedRecommendation {
  id: string; // Unique identifier for React keys
  type: RecommendationType;
  rating: number; // For sorting (rate for split/merge/move, 0.3 default for violations)
  title: string;
  description: string;
  data: SplitModuleRecommendation | MergeModuleRecommendation | MoveClassRecommendation | ArchitectureViolationRecommendation;
}

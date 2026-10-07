import { Dependency } from './Dependency.tsx';

export type EnforceMode = 'ALLOW' | 'MUST';

// Mirrors ClazzResponseDTO in api/.
export interface ClazzData {
  id: string;
  name: string;
  dependencies: Dependency[];
  similarity: number;
  avgSimilarityWithRefClazzes: number;
  firstModule: string;
  currentModule: string;
  // Null until a class is made a reference class.
  enforceMode: EnforceMode | null;
}

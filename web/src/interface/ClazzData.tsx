import { Dependency } from './Dependency.tsx';

export type EnforceMode = 'ALLOW' | 'MUST';

// Mirrors ClazzResponseDTO in api/.
export interface ClazzData {
  id: string;
  name: string;
  // The package its source file declares; null for the default package or an unreadable file.
  // With `name` it gives the class's fully qualified name, which maps a dependency to its module.
  packageName: string | null;
  dependencies: Dependency[];
  similarity: number;
  avgSimilarityWithRefClazzes: number;
  firstModule: string;
  currentModule: string;
  // Null until a class is made a reference class.
  enforceMode: EnforceMode | null;
}

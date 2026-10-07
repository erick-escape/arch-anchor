import { ClazzData } from './ClazzData.tsx';
import { Dependency } from './Dependency.tsx';

// Mirrors ModuleDTO in api/.
export interface ModuleData {
  id: string;
  name: string;
  refClazzes: ClazzData[];
  refClazzesDependencies: Dependency[];
  // Always null from /api/analyze today: ModuleMapper does not map Module.moduleDependencies (#8).
  allDependencies: Dependency[] | null;
  clazzes: ClazzData[];
  similarity: number;
  avgRefClazzesSimilarity: number;
}

// Mirrors SplitModuleResponse in api/.
export interface SplitModuleResponse {
  newModules: ModuleData[];
}

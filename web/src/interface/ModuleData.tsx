import { ClazzData } from './ClazzData.tsx';
import { Dependency } from './Dependency.tsx';

// Mirrors ModuleDTO in api/.
export interface ModuleData {
  id: string;
  name: string;
  refClazzes: ClazzData[];
  refClazzesDependencies: Dependency[];
  allDependencies: Dependency[];
  clazzes: ClazzData[];
  similarity: number;
  avgRefClazzesSimilarity: number;
}

// Mirrors SplitModuleResponse in api/.
export interface SplitModuleResponse {
  newModules: ModuleData[];
}

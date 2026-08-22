import { ClazzData } from './ClazzData.tsx';
import { Dependency } from './Dependency.tsx';

export interface ModuleData {
  id: string;
  name: string;
  refClass: string;
  refClazzes: ClazzData[];
  clazzes: ClazzData[];
  dependencies: Dependency[];
  similarity: number;
}

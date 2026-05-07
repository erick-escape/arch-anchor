import { Dependency } from './Dependency.tsx';

export interface ClazzData {
  id: string;
  name: string;
  dependencies: Dependency[];
  similarity: number;
  firstModule: string;
  currentModule: string;
}

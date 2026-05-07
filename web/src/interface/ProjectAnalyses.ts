import { ModuleData } from './ModuleData';

export interface ArchitecturalConstraint {
  moduleId: string;
  moduleName: string;
  refClazzesDependencies: {
    packageName: string;
    types: { fullyQualifiedName: string; className: string }[];
  }[];
}

export interface ProjectAnalyses {
  id: string;
  projectName: string;
  modulesList: ModuleData[];
  projectSimilarity: number;
  architecturalConstraints: ArchitecturalConstraint[];
}

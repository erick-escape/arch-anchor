import { ModuleData } from './ModuleData';

export interface RefClassConstraint {
  refClassId: string;
  refClassName: string;
  enforceMode: 'ALLOW' | 'MUST';
  dependencies: {
    packageName: string;
    types: { fullyQualifiedName: string; className: string }[];
  }[];
}

export interface ArchitecturalConstraint {
  moduleId: string;
  moduleName: string;
  refClassConstraints: RefClassConstraint[];
}

export interface ProjectAnalyses {
  id: string;
  projectName: string;
  modulesList: ModuleData[];
  projectSimilarity: number;
  architecturalConstraints: ArchitecturalConstraint[];
}

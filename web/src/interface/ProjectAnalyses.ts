import { EnforceMode } from './ClazzData';
import { Dependency } from './Dependency';
import { ModuleData } from './ModuleData';

export interface RefClassConstraint {
  refClassId: string;
  refClassName: string;
  enforceMode: EnforceMode;
  dependencies: Dependency[];
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

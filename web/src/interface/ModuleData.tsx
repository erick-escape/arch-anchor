import { ClassData } from './ClassData.tsx';
import { Dependency } from './Dependency.tsx';

export interface ModuleData {
    id: string;
    name: string;
    refClass: string;
    classes: ClassData[];
    dependencies: Dependency[];
    similarity: number;
}
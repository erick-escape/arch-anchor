import { ClassData } from './ClassData.tsx';
import { TypeData } from './Type.tsx';

export interface ModuleData {
    id: number;
    name: string;
    refClass: string;
    classes: ClassData[];
    types: TypeData[];
    similarity: number;
}
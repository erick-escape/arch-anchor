import { Dependency } from './Dependency.tsx';

export interface ClassData {
    id: string;
    name: string;
    dependencies: Dependency[];
    similarity: number;
    firstModule: string;
    currentModule: string;
}
import { AttributeData } from './AttributeData.tsx';
import { MethodData } from './MethodData.tsx';
import { TypeData } from './Type.tsx';

export interface ClassData {
    id: number;
    name: string;
    methods: MethodData[];
    attributes: AttributeData[];
    types: TypeData[];
    similarity: number;
}
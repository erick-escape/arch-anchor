import { Dependency } from './Dependency.tsx';

export type EnforceMode = 'ALLOW' | 'MUST';

// Mirrors ClazzResponseDTO in api/.
export interface ClazzData {
  id: string;
  name: string;
  // The package its source file declares; null for the default package or an unreadable file.
  packageName: string | null;
  // The name other classes refer to it by, written as the project's language writes it
  // (bookstore.web.SalesReport, App\Models\User); it maps a dependency to its module (#83).
  // Null for a class nothing can refer to, such as a Java class of the default package.
  qualifiedName: string | null;
  dependencies: Dependency[];
  similarity: number;
  avgSimilarityWithRefClazzes: number;
  firstModule: string;
  currentModule: string;
  // Null until a class is made a reference class.
  enforceMode: EnforceMode | null;
}

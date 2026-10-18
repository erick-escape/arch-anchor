import type { DependencyStatus } from '../../../components/vocabulary/dependencyStatus';
import type { ClazzData } from '../../../interface/ClazzData';
import type { ModuleData } from '../../../interface/ModuleData';

/**
 * A module's contract: the packages its reference classes depend on. Every other package the
 * module uses is a violation, as the backend counts it (ArchitectureViolation).
 */
export function contractOf(module: ModuleData): Set<string> {
  return new Set(module.refClazzesDependencies.map((dependency) => dependency.packageName));
}

/**
 * The packages a module uses outside its contract, in the order the API lists them.
 *
 * @example violationsOf(bookstoreWeb) // ['java.sql']
 */
export function violationsOf(module: ModuleData): string[] {
  const contract = contractOf(module);
  return module.allDependencies
    .map((dependency) => dependency.packageName)
    .filter((packageName) => !contract.has(packageName));
}

/** The violations of every module, the number the Analyze heading states. */
export function totalViolationsOf(modules: ModuleData[]): number {
  return modules.reduce((total, module) => total + violationsOf(module).length, 0);
}

/**
 * The project's similarity, computed as the API does: the mean of its modules' similarities. Kept
 * here because the saved modules the page reloads after a change come without it.
 *
 * @example projectSimilarityOf(bookstore) // 0.9375
 */
export function projectSimilarityOf(modules: ModuleData[]): number {
  if (modules.length === 0) return 0;
  return modules.reduce((sum, module) => sum + module.similarity, 0) / modules.length;
}

/**
 * How a class stands against its module's contract: divergence when it uses a forbidden package,
 * absence when it lacks a dependency a MUST reference class requires, nothing when it conforms.
 *
 * @example classStatusOf(salesReport, bookstoreWeb) // 'divergence'
 */
export function classStatusOf(clazz: ClazzData, module: ModuleData): DependencyStatus | undefined {
  const contract = contractOf(module);
  const used = new Set(clazz.dependencies.map((dependency) => dependency.packageName));
  if ([...used].some((packageName) => !contract.has(packageName))) return 'divergence';
  if (missesMandatoryDependency(used, module)) return 'absence';
  return undefined;
}

function missesMandatoryDependency(used: Set<string>, module: ModuleData): boolean {
  const mandatory = module.refClazzes
    .filter((reference) => reference.enforceMode === 'MUST')
    .flatMap((reference) => reference.dependencies.map((dependency) => dependency.packageName));
  return mandatory.some((packageName) => !used.has(packageName));
}

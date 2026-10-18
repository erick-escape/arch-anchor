import type { Dependency } from '../../../interface/Dependency';
import type { ModuleData } from '../../../interface/ModuleData';
import { contractOf } from './moduleContract';

export type EdgeKind = 'allowed' | 'divergence';

export interface DependencyEdgeSpec {
  id: string;
  source: string;
  target: string;
  kind: EdgeKind;
  /** The packages behind the edge, for its label and tooltip. */
  packages: string[];
}

export interface ExternalPackageSpec {
  id: string;
  name: string;
  /** Some module uses it outside its contract. */
  violated: boolean;
}

export interface DependencyGraph {
  edges: DependencyEdgeSpec[];
  packages: ExternalPackageSpec[];
}

const PACKAGE_PREFIX = 'package:';

/**
 * Every class of the project by fully qualified name, mapped to the id of the module holding it.
 * Module names cannot be used: they become "a | b" after a merge and "_Retained" after a split.
 */
export function classOwnerIndex(modules: ModuleData[]): Map<string, string> {
  const index = new Map<string, string>();
  for (const module of modules) {
    for (const clazz of module.clazzes) {
      if (clazz.packageName) index.set(`${clazz.packageName}.${clazz.name}`, module.id);
    }
  }
  return index;
}

/**
 * The module holding a type, walking out of nested types to their enclosing class, as
 * ModuleScope.isInternal does in the API.
 *
 * @example owningModuleOf('bookstore.model.Book.Builder', index) // the id of bookstore/model
 */
export function owningModuleOf(
  fullyQualifiedName: string,
  index: Map<string, string>
): string | undefined {
  for (
    let name = fullyQualifiedName;
    name.includes('.');
    name = name.slice(0, name.lastIndexOf('.'))
  ) {
    const owner = index.get(name);
    if (owner) return owner;
  }
  return undefined;
}

/**
 * The module graph: an edge from each module to every module and external package it depends
 * on, marked divergence when its contract forbids the package. External packages are shown only
 * for violations unless `showExternal` asks for all of them.
 *
 * @example dependencyGraphOf(modules, { showExternal: false })
 */
export function dependencyGraphOf(
  modules: ModuleData[],
  options: { showExternal: boolean }
): DependencyGraph {
  const builder = new GraphBuilder(classOwnerIndex(modules), options.showExternal);
  for (const module of modules) builder.addModule(module);
  return builder.graph();
}

// Collects the edges and external packages of one dependency graph.
class GraphBuilder {
  private readonly edges = new Map<string, DependencyEdgeSpec>();
  private readonly packages = new Map<string, ExternalPackageSpec>();

  constructor(
    private readonly index: Map<string, string>,
    private readonly showExternal: boolean
  ) {}

  addModule(module: ModuleData): void {
    const contract = contractOf(module);
    for (const dependency of module.allDependencies) {
      const kind: EdgeKind = contract.has(dependency.packageName) ? 'allowed' : 'divergence';
      this.addDependency(module.id, dependency, kind);
    }
  }

  graph(): DependencyGraph {
    return { edges: [...this.edges.values()], packages: [...this.packages.values()] };
  }

  private addDependency(source: string, dependency: Dependency, kind: EdgeKind): void {
    const { packageName } = dependency;
    for (const target of targetsOf(dependency, source, this.index)) {
      addEdge(this.edges, source, target, kind, packageName);
    }
    if (!isExternal(dependency, this.index)) return;
    if (!this.showExternal && kind === 'allowed') return;
    addPackage(this.packages, packageName, kind === 'divergence');
    addEdge(this.edges, source, PACKAGE_PREFIX + packageName, kind, packageName);
  }
}

function targetsOf(
  dependency: Dependency,
  source: string,
  index: Map<string, string>
): Set<string> {
  const owners = dependency.types.map((type) => owningModuleOf(type.fullyQualifiedName, index));
  return new Set(
    owners.filter((owner): owner is string => owner !== undefined && owner !== source)
  );
}

function isExternal(dependency: Dependency, index: Map<string, string>): boolean {
  return dependency.types.every(
    (type) => owningModuleOf(type.fullyQualifiedName, index) === undefined
  );
}

function addEdge(
  edges: Map<string, DependencyEdgeSpec>,
  source: string,
  target: string,
  kind: EdgeKind,
  packageName: string
): void {
  const id = `${source}->${target}`;
  const existing = edges.get(id);
  if (!existing) {
    edges.set(id, { id, source, target, kind, packages: [packageName] });
    return;
  }
  if (!existing.packages.includes(packageName)) existing.packages.push(packageName);
  if (kind === 'divergence') existing.kind = 'divergence';
}

function addPackage(
  packages: Map<string, ExternalPackageSpec>,
  name: string,
  violated: boolean
): void {
  const existing = packages.get(name);
  packages.set(name, {
    id: PACKAGE_PREFIX + name,
    name,
    violated: violated || Boolean(existing?.violated),
  });
}

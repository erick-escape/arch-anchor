import { describe, expect, it } from 'vitest';
import type { ClazzData } from '../../../interface/ClazzData';
import type { Dependency } from '../../../interface/Dependency';
import { bookstoreModules } from '../../../test/fixtures/bookstoreFixture';
import { clazzFixture, moduleFixture } from '../../../test/fixtures/moduleFixtures';
import { classOwnerIndex, dependencyGraphOf, owningModuleOf } from './moduleEdges';

// A class named as PHP names it: nothing in the graph may assume a name is written with dots (#83).
function phpClazz(namespace: string, name: string): ClazzData {
  return clazzFixture({
    id: name,
    name,
    packageName: namespace,
    qualifiedName: `${namespace}\\${name}`,
  });
}

describe('owningModuleOf', () => {
  const index = classOwnerIndex(bookstoreModules());

  it('finds the module of a class by its fully qualified name', () => {
    expect(owningModuleOf('bookstore.model.Book', index)).toBe('model');
  });

  it('finds the module of a nested type through its enclosing class', () => {
    expect(owningModuleOf('bookstore.model.Book.Builder', index)).toBe('model');
  });

  it('finds no module for a type outside the project', () => {
    expect(owningModuleOf('java.sql.Connection', index)).toBeUndefined();
  });

  it('finds a class by the name its parser gave it, however the language writes names', () => {
    const models = moduleFixture({ id: 'models', clazzes: [phpClazz('App\\Models', 'User')] });
    const phpIndex = classOwnerIndex([models]);

    expect(owningModuleOf('App\\Models\\User', phpIndex)).toBe('models');
    expect(owningModuleOf('App\\Models\\Order', phpIndex)).toBeUndefined();
  });

  it('finds a class that is in no package by its bare name', () => {
    const mailer = clazzFixture({ name: 'Mailer', packageName: null, qualifiedName: 'Mailer' });
    const legacy = moduleFixture({ id: 'legacy', clazzes: [mailer] });

    expect(owningModuleOf('Mailer', classOwnerIndex([legacy]))).toBe('legacy');
  });

  it('leaves out a class nothing can refer to', () => {
    const launcher = clazzFixture({ name: 'Launcher', packageName: null, qualifiedName: null });

    expect(classOwnerIndex([moduleFixture({ clazzes: [launcher] })]).size).toBe(0);
  });
});

describe('dependencyGraphOf', () => {
  it('draws one edge per pair of dependent modules, allowed when the contract permits it', () => {
    const { edges } = dependencyGraphOf(bookstoreModules(), { showExternal: false });

    const pairs = edges.map((edge) => `${edge.source}->${edge.target}:${edge.kind}`);
    expect(pairs).toEqual(
      expect.arrayContaining([
        'web->service:allowed',
        'web->model:allowed',
        'service->persistence:allowed',
        'service->model:allowed',
        'persistence->model:allowed',
      ])
    );
  });

  it('shows the external package of a violation, and only that one by default', () => {
    const { packages, edges } = dependencyGraphOf(bookstoreModules(), { showExternal: false });

    expect(packages).toEqual([{ id: 'package:java.sql', name: 'java.sql', violated: true }]);
    expect(edges).toContainEqual(
      expect.objectContaining({ source: 'web', target: 'package:java.sql', kind: 'divergence' })
    );
  });

  it('can show every external package, allowed ones solid', () => {
    const { packages, edges } = dependencyGraphOf(bookstoreModules(), { showExternal: true });

    expect(packages.map((pkg) => pkg.name).sort()).toEqual(['java.sql', 'java.time']);
    expect(edges).toContainEqual(
      expect.objectContaining({
        source: 'persistence',
        target: 'package:java.sql',
        kind: 'allowed',
      })
    );
  });

  it('draws the PHP bookstore with the edges of the Java one, ext-pdo standing for java.sql', () => {
    const java = dependencyGraphOf(bookstoreModules(), { showExternal: false });
    const php = dependencyGraphOf(bookstoreModules({ language: 'php' }), { showExternal: false });

    const pairsOf = (graph: typeof java) =>
      graph.edges.map((edge) => `${edge.source}->${edge.target}:${edge.kind}`);
    expect(pairsOf(php)).toEqual(
      pairsOf(java).map((pair) => pair.replace('package:java.sql', 'package:ext-pdo'))
    );
    expect(php.packages).toEqual([{ id: 'package:ext-pdo', name: 'ext-pdo', violated: true }]);
  });

  it('draws the edges of a project whose names are not written with dots', () => {
    const user = phpClazz('App\\Models', 'User');
    const controller = phpClazz('App\\Http\\Controllers', 'UserController');
    const usesModels: Dependency = {
      packageName: 'App\\Models',
      types: [{ fullyQualifiedName: 'App\\Models\\User', className: 'User' }],
    };
    const models = moduleFixture({ id: 'models', refClazzes: [user], clazzes: [user] });
    const controllers = moduleFixture({
      id: 'controllers',
      refClazzes: [controller],
      clazzes: [controller],
      refClazzesDependencies: [usesModels],
      allDependencies: [usesModels],
    });

    const { edges, packages } = dependencyGraphOf([models, controllers], { showExternal: true });

    expect(edges.map((edge) => `${edge.source}->${edge.target}:${edge.kind}`)).toEqual([
      'controllers->models:allowed',
    ]);
    expect(packages).toEqual([]);
  });

  it('leaves no violation once SalesReport has moved', () => {
    const { edges } = dependencyGraphOf(bookstoreModules({ afterMove: true }), {
      showExternal: false,
    });

    expect(edges.every((edge) => edge.kind === 'allowed')).toBe(true);
  });
});

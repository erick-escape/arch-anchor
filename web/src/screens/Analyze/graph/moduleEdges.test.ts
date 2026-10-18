import { describe, expect, it } from 'vitest';
import { bookstoreModules } from '../../../test/fixtures/bookstoreFixture';
import { classOwnerIndex, dependencyGraphOf, owningModuleOf } from './moduleEdges';

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

  it('leaves no violation once SalesReport has moved', () => {
    const { edges } = dependencyGraphOf(bookstoreModules({ afterMove: true }), {
      showExternal: false,
    });

    expect(edges.every((edge) => edge.kind === 'allowed')).toBe(true);
  });
});

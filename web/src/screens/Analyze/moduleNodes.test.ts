import { describe, expect, it } from 'vitest';
import {
  layoutModulesConcentric,
  rebuildNodesPreservingPositions,
  toModuleNode,
  withHighlightedNodes,
  withoutIds,
  withRenamedModule,
  withRenamedModuleNode,
  withSplitModuleNodes,
} from './moduleNodes';
import { moduleFixture } from '../../test/fixtures/moduleFixtures';

const center = { x: 1000, y: 500 };

function modulesNamed(count: number) {
  return Array.from({ length: count }, (_, index) =>
    moduleFixture({ id: `m${index}`, name: `module${index}` })
  );
}

describe('toModuleNode', () => {
  it('wraps a module in a draggable custom node keyed by the module id', () => {
    const module = moduleFixture({ id: 'm7' });

    expect(toModuleNode(module, { x: 1, y: 2 })).toEqual({
      id: 'm7',
      type: 'customNode',
      data: { module },
      position: { x: 1, y: 2 },
      draggable: true,
    });
  });
});

describe('layoutModulesConcentric', () => {
  it('returns no nodes for no modules', () => {
    expect(layoutModulesConcentric([], center)).toEqual([]);
  });

  it('puts the first module in the centre', () => {
    const [first] = layoutModulesConcentric(modulesNamed(3), center);

    expect(first.position).toEqual(center);
  });

  it('fills the first ring, 200px out, before opening the second', () => {
    // Ring 1 holds floor(2π·200 / 150) = 8 nodes, so module 9 is the first on ring 2.
    const nodes = layoutModulesConcentric(modulesNamed(10), center);

    expect(nodes[1].position).toEqual({ x: 1200, y: 500 });
    expect(nodes[9].position).toEqual({ x: 1400, y: 500 });
    expect(nodes.map((node) => node.id)).toEqual(modulesNamed(10).map((module) => module.id));
  });
});

describe('withoutIds', () => {
  it('drops the listed ids and keeps the rest in order', () => {
    const modules = modulesNamed(4);

    expect(withoutIds(modules, ['m1', 'm3']).map((module) => module.id)).toEqual(['m0', 'm2']);
  });
});

describe('renaming', () => {
  it('renames a module without mutating the previous state', () => {
    const modules = modulesNamed(2);

    const renamed = withRenamedModule(modules, 'm1', 'billing');

    expect(renamed[1].name).toBe('billing');
    expect(modules[1].name).toBe('module1');
  });

  it('renames the module carried by a node without mutating the previous state', () => {
    const nodes = layoutModulesConcentric(modulesNamed(2), center);

    const renamed = withRenamedModuleNode(nodes, 'm0', 'billing');

    expect(renamed[0].data.module.name).toBe('billing');
    expect(nodes[0].data.module.name).toBe('module0');
    expect(renamed[1]).toBe(nodes[1]);
  });
});

describe('withSplitModuleNodes', () => {
  it('replaces the split node with the new modules either side of its position', () => {
    const nodes = [toModuleNode(moduleFixture({ id: 'old' }), { x: 300, y: 40 })];
    const halves = [moduleFixture({ id: 'a' }), moduleFixture({ id: 'b' })];

    const result = withSplitModuleNodes(nodes, 'old', halves);

    expect(result.map((node) => [node.id, node.position])).toEqual([
      ['a', { x: 200, y: 40 }],
      ['b', { x: 400, y: 40 }],
    ]);
  });
});

describe('withHighlightedNodes', () => {
  it('highlights exactly the listed nodes', () => {
    const nodes = layoutModulesConcentric(modulesNamed(3), center);

    const result = withHighlightedNodes(nodes, ['m2']);

    expect(result.map((node) => node.className)).toEqual(['', '', 'highlight']);
  });
});

describe('rebuildNodesPreservingPositions', () => {
  it('keeps the position of modules that still exist and refreshes their data', () => {
    const before = [toModuleNode(moduleFixture({ id: 'm0', similarity: 0.1 }), { x: 5, y: 6 })];
    const refreshed = moduleFixture({ id: 'm0', similarity: 0.9 });

    const [node] = rebuildNodesPreservingPositions(before, [refreshed], center);

    expect(node.position).toEqual({ x: 5, y: 6 });
    expect(node.data.module).toBe(refreshed);
  });

  it('drops removed modules and places new ones on a 300px circle', () => {
    const before = [toModuleNode(moduleFixture({ id: 'gone' }), { x: 5, y: 6 })];

    const nodes = rebuildNodesPreservingPositions(before, [moduleFixture({ id: 'new' })], center);

    expect(nodes.map((node) => [node.id, node.position])).toEqual([['new', { x: 1300, y: 500 }]]);
  });
});

import { describe, expect, it } from 'vitest';
import { bookstoreModules } from '../../../test/fixtures/bookstoreFixture';
import { moduleFixture } from '../../../test/fixtures/moduleFixtures';
import {
  dependencyEdgesOf,
  graphNodesOf,
  moduleNodeWidth,
  nextGraphNodes,
  withEmphasis,
  withHighlightedNodes,
} from './graphElements';
import { dependencyGraphOf } from './moduleEdges';

const modules = bookstoreModules();
const graph = dependencyGraphOf(modules, { showExternal: false });

describe('graphNodesOf', () => {
  it('makes one module node per module and one package node per shown external package', () => {
    const nodes = graphNodesOf(modules, graph);

    expect(nodes.filter((node) => node.type === 'module').map((node) => node.id)).toEqual([
      'model',
      'persistence',
      'service',
      'web',
    ]);
    expect(nodes.filter((node) => node.type === 'package').map((node) => node.id)).toEqual([
      'package:java.sql',
    ]);
  });

  it('lays the layers out top to bottom: web above service above persistence above model', () => {
    const y = new Map(graphNodesOf(modules, graph).map((node) => [node.id, node.position.y]));

    expect(y.get('web')!).toBeLessThan(y.get('service')!);
    expect(y.get('service')!).toBeLessThan(y.get('persistence')!);
    expect(y.get('persistence')!).toBeLessThan(y.get('model')!);
  });
});

describe('nextGraphNodes', () => {
  it('keeps every position the user arranged when the same modules come back changed', () => {
    const arranged = graphNodesOf(modules, graph).map((node) => ({
      ...node,
      position: { x: 7, y: 9 },
    }));
    const afterMove = bookstoreModules({ afterMove: true });

    const next = nextGraphNodes(
      arranged,
      afterMove,
      dependencyGraphOf(afterMove, { showExternal: false })
    );

    const web = next.find((node) => node.id === 'web');
    expect(web?.position).toEqual({ x: 7, y: 9 });
    expect(web?.type === 'module' && web.data.module.clazzes).toHaveLength(3);
  });

  it('lays the graph out again when modules appear or go, as after a split or a merge', () => {
    const arranged = graphNodesOf(modules, graph).map((node) => ({
      ...node,
      position: { x: 7, y: 9 },
    }));
    const merged = [...modules.slice(0, 3), moduleFixture({ id: 'web+x', name: 'web | x' })];

    const next = nextGraphNodes(
      arranged,
      merged,
      dependencyGraphOf(merged, { showExternal: false })
    );

    expect(next.some((node) => node.position.x !== 7)).toBe(true);
  });
});

describe('dependencyEdgesOf', () => {
  it('names each edge for screen readers with the modules and the packages behind it', () => {
    const edges = dependencyEdgesOf(graph.edges, modules);

    const violation = edges.find((edge) => edge.target === 'package:java.sql');
    expect(violation?.data?.kind).toBe('divergence');
    expect(violation?.ariaLabel).toBe('bookstore/web depends on java.sql: violation');
    expect(edges.find((edge) => edge.id === 'web->service')?.ariaLabel).toBe(
      'bookstore/web depends on bookstore/service (bookstore.service)'
    );
  });
});

describe('moduleNodeWidth', () => {
  it('fits up to six class tiles in a row, and never less than two', () => {
    expect(moduleNodeWidth(moduleFixture())).toBe(moduleNodeWidth(moduleFixture({ clazzes: [] })));
    expect(moduleNodeWidth(modules[3])).toBeLessThan(
      moduleNodeWidth(
        moduleFixture({ clazzes: Array.from({ length: 9 }, () => modules[3].clazzes[0]) })
      )
    );
  });
});

describe('withHighlightedNodes', () => {
  it('marks the merge targets under a dragged module and clears the rest', () => {
    const nodes = graphNodesOf(modules, graph);

    const marked = withHighlightedNodes(nodes, ['web']);

    expect(marked.find((node) => node.id === 'web')?.className).toBe('highlight');
    expect(marked.find((node) => node.id === 'model')?.className).toBe('');
  });
});

describe('withEmphasis', () => {
  const nodes = graphNodesOf(modules, graph);

  it('lights the nodes a hovered recommendation touches and leaves the rest as they were', () => {
    const lit = withEmphasis(nodes, new Set(['web', 'package:java.sql']), new Set());

    expect(lit.find((node) => node.id === 'web')?.data.highlighted).toBe(true);
    expect(lit.find((node) => node.id === 'package:java.sql')?.data.highlighted).toBe(true);
    expect(lit.find((node) => node.id === 'model')).toBe(nodes.find((node) => node.id === 'model'));
  });

  it('pulses the nodes a change just reached, keeping a merge highlight', () => {
    const merging = withHighlightedNodes(nodes, ['persistence']);

    const landed = withEmphasis(merging, new Set(), new Set(['persistence']));

    expect(landed.find((node) => node.id === 'persistence')?.className).toBe('highlight flash');
  });
});

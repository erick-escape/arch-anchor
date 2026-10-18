import type { Edge, Node } from '@xyflow/react';
import type { ModuleData } from '../../../interface/ModuleData';
import { layeredPositions } from './graphLayout';
import type { DependencyEdgeSpec, DependencyGraph, EdgeKind } from './moduleEdges';

// Type aliases, not interfaces: React Flow requires node and edge data to be Record<string, unknown>.
export type ModuleNodeData = { module: ModuleData; highlighted: boolean };
export type PackageNodeData = { name: string; violated: boolean };
export type ModuleNode = Node<ModuleNodeData, 'module'>;
export type PackageNode = Node<PackageNodeData, 'package'>;
export type GraphNode = ModuleNode | PackageNode;
export type DependencyEdgeData = { kind: EdgeKind; packages: string[] };
export type DependencyEdge = Edge<DependencyEdgeData, 'dependency'>;

// A module node's geometry: up to six 28 px class tiles per row, 12 px apart, 20 px padding.
const TILE = 28;
const TILE_GAP = 12;
const TILES_PER_ROW = 6;
const MODULE_PADDING = 40;
const MODULE_HEIGHT = 128;
const PACKAGE_WIDTH = 132;
const PACKAGE_HEIGHT = 32;

/**
 * The width a module node is drawn at, from its number of classes: room for up to six tiles a
 * row, and at least two, so the name tab and the meter fit.
 */
export function moduleNodeWidth(module: ModuleData): number {
  const columns = Math.min(Math.max(module.clazzes.length, 4), TILES_PER_ROW);
  return MODULE_PADDING + columns * TILE + (columns - 1) * TILE_GAP;
}

function moduleNodeHeight(module: ModuleData): number {
  const rows = Math.min(Math.ceil(module.clazzes.length / TILES_PER_ROW), 2);
  return MODULE_HEIGHT + Math.max(rows - 1, 0) * (TILE + TILE_GAP);
}

function moduleNode(module: ModuleData): ModuleNode {
  const width = moduleNodeWidth(module);
  const data = { module, highlighted: false };
  return { id: module.id, type: 'module', data, position: { x: 0, y: 0 }, width, style: { width } };
}

function packageNode(name: string, violated: boolean, id: string): PackageNode {
  return {
    id,
    type: 'package',
    data: { name, violated },
    position: { x: 0, y: 0 },
    width: PACKAGE_WIDTH,
  };
}

/**
 * The graph's nodes, laid out in layers: a module above the modules and packages it depends on.
 *
 * @example setNodes(graphNodesOf(modules, dependencyGraphOf(modules, { showExternal: false })))
 */
export function graphNodesOf(modules: ModuleData[], graph: DependencyGraph): GraphNode[] {
  const nodes: GraphNode[] = [
    ...modules.map(moduleNode),
    ...graph.packages.map((pkg) => packageNode(pkg.name, pkg.violated, pkg.id)),
  ];
  const heights = new Map(modules.map((module) => [module.id, moduleNodeHeight(module)]));
  const boxes = nodes.map((node) => ({
    id: node.id,
    width: node.width ?? PACKAGE_WIDTH,
    height: heights.get(node.id) ?? PACKAGE_HEIGHT,
  }));
  const positions = layeredPositions(boxes, graph.edges);
  return nodes.map((node) => ({ ...node, position: positions.get(node.id) ?? node.position }));
}

/**
 * The nodes for a new version of the modules. When the same modules come back (a move, a new
 * reference class, new weights), every node keeps the position the user gave it; when modules
 * appear or go (a split, a merge, a delete), the graph is laid out again.
 */
export function nextGraphNodes(
  current: GraphNode[],
  modules: ModuleData[],
  graph: DependencyGraph
): GraphNode[] {
  const next = graphNodesOf(modules, graph);
  if (!sameModules(current, next)) return next;
  const positions = new Map(current.map((node) => [node.id, node.position]));
  return next.map((node) => ({ ...node, position: positions.get(node.id) ?? node.position }));
}

// Only modules decide: a package node coming or going (a violation fixed by a move) must not
// throw away the arrangement.
function sameModules(first: GraphNode[], second: GraphNode[]): boolean {
  const moduleIds = (nodes: GraphNode[]) =>
    nodes.filter((node) => node.type === 'module').map((node) => node.id);
  const ids = new Set(moduleIds(first));
  const next = moduleIds(second);
  return ids.size === next.length && next.every((id) => ids.has(id));
}

/**
 * The edges React Flow draws, each named for screen readers.
 *
 * @example dependencyEdgesOf(graph.edges, modules)
 */
export function dependencyEdgesOf(
  specs: DependencyEdgeSpec[],
  modules: ModuleData[]
): DependencyEdge[] {
  const names = new Map(modules.map((module) => [module.id, module.name]));
  return specs.map((spec) => ({
    id: spec.id,
    source: spec.source,
    target: spec.target,
    type: 'dependency',
    data: { kind: spec.kind, packages: spec.packages },
    ariaLabel: edgeLabelOf(spec, names),
  }));
}

function edgeLabelOf(spec: DependencyEdgeSpec, names: Map<string, string>): string {
  const source = names.get(spec.source) ?? spec.source;
  const packages = spec.packages.join(', ');
  const target = names.get(spec.target);
  const what = target ? `${target} (${packages})` : packages;
  return `${source} depends on ${what}${spec.kind === 'divergence' ? ': violation' : ''}`;
}

/**
 * Marks the modules a dragged module would merge into (the `highlight` class in xy-theme.css)
 * and clears the rest.
 */
export function withHighlightedNodes(nodes: GraphNode[], ids: string[]): GraphNode[] {
  return nodes.map((node) => ({ ...node, className: ids.includes(node.id) ? 'highlight' : '' }));
}

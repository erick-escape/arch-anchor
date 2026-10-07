import { Node, XYPosition } from '@xyflow/react';
import { ModuleData } from '../../interface/ModuleData';

// A type alias, not an interface: React Flow requires node data to be a Record<string, unknown>.
export type ModuleNodeData = { module: ModuleData };
export type ModuleNode = Node<ModuleNodeData, 'customNode'>;

// Concentric layout: rings 200px apart, each holding as many ~150px-wide nodes as fit.
const RING_GAP = 200;
const AVERAGE_NODE_WIDTH = 150;
// New modules that appear after a refresh are placed on a single circle of this radius.
const REFRESH_RADIUS = 300;
// Distance either side of a split module's position at which its two halves are placed.
const SPLIT_OFFSET = 100;

/**
 * Wraps a module in the React Flow node that renders it.
 *
 * @example
 * setNodes((nodes) => [...nodes, toModuleNode(merged, target.position)]);
 */
export function toModuleNode(module: ModuleData, position: XYPosition): ModuleNode {
  return { id: module.id, type: 'customNode', data: { module }, position, draggable: true };
}

/**
 * Places the first module at the centre and the rest on concentric rings around it.
 *
 * @example
 * setNodes(layoutModulesConcentric(modules, { x: innerWidth / 2, y: innerHeight / 2 }));
 */
export function layoutModulesConcentric(modules: ModuleData[], center: XYPosition): ModuleNode[] {
  return modules.map((module, index) =>
    toModuleNode(module, index === 0 ? center : concentricPosition(index, center))
  );
}

// Position of the index-th module (1-based after the centre) on the ring that holds it.
function concentricPosition(index: number, center: XYPosition): XYPosition {
  let firstIndexOnRing = 1;
  for (let ring = 1; ; ring++) {
    const radius = ring * RING_GAP;
    const capacity = Math.max(Math.floor((2 * Math.PI * radius) / AVERAGE_NODE_WIDTH), 1);
    if (index < firstIndexOnRing + capacity) {
      return onCircle(center, radius, (index - firstIndexOnRing) / capacity);
    }
    firstIndexOnRing += capacity;
  }
}

function onCircle(center: XYPosition, radius: number, turn: number): XYPosition {
  const angle = 2 * Math.PI * turn;
  return { x: center.x + radius * Math.cos(angle), y: center.y + radius * Math.sin(angle) };
}

/**
 * Removes the items with the given ids; works for both modules and module nodes.
 *
 * @example
 * setModules((modules) => withoutIds(modules, [deletedId]));
 */
export function withoutIds<T extends { id: string }>(items: T[], ids: string[]): T[] {
  return items.filter((item) => !ids.includes(item.id));
}

/**
 * Returns the modules with one renamed, leaving the previous array and module untouched.
 *
 * @example
 * setModules((modules) => withRenamedModule(modules, 'm1', 'billing'));
 */
export function withRenamedModule(
  modules: ModuleData[],
  moduleId: string,
  newName: string
): ModuleData[] {
  return modules.map((module) => (module.id === moduleId ? { ...module, name: newName } : module));
}

/**
 * Returns the nodes with one module renamed, leaving the previous nodes untouched.
 *
 * @example
 * setNodes((nodes) => withRenamedModuleNode(nodes, 'm1', 'billing'));
 */
export function withRenamedModuleNode(
  nodes: ModuleNode[],
  moduleId: string,
  newName: string
): ModuleNode[] {
  return nodes.map((node) =>
    node.id === moduleId
      ? { ...node, data: { module: { ...node.data.module, name: newName } } }
      : node
  );
}

/**
 * Replaces a split module's node with its new modules, placed either side of it.
 *
 * @example
 * setNodes((nodes) => withSplitModuleNodes(nodes, oldId, response.newModules));
 */
export function withSplitModuleNodes(
  nodes: ModuleNode[],
  oldModuleId: string,
  newModules: ModuleData[]
): ModuleNode[] {
  const base = nodes.find((node) => node.id === oldModuleId)?.position ?? { x: 0, y: 0 };
  const splitNodes = newModules.map((module, index) =>
    toModuleNode(module, {
      x: base.x + (index === 0 ? -SPLIT_OFFSET : SPLIT_OFFSET),
      y: base.y,
    })
  );
  return [...withoutIds(nodes, [oldModuleId]), ...splitNodes];
}

/**
 * Marks the given nodes as merge targets (the `highlight` class in App.css) and clears the rest.
 *
 * @example
 * setNodes((nodes) => withHighlightedNodes(nodes, intersectingIds));
 */
export function withHighlightedNodes(nodes: ModuleNode[], ids: string[]): ModuleNode[] {
  return nodes.map((node) => ({ ...node, className: ids.includes(node.id) ? 'highlight' : '' }));
}

/**
 * Rebuilds the nodes for a fresh module list. Modules that already had a node keep its position
 * (and any size the user gave it); new modules go on a circle around the centre.
 *
 * @example
 * setNodes((nodes) => rebuildNodesPreservingPositions(nodes, analyses.modulesList, center));
 */
export function rebuildNodesPreservingPositions(
  currentNodes: ModuleNode[],
  modules: ModuleData[],
  center: XYPosition
): ModuleNode[] {
  const existing = new Map(currentNodes.map((node) => [node.id, node]));
  return modules.map((module, index) => {
    const node = existing.get(module.id);
    if (node) return { ...node, data: { module } };
    return toModuleNode(module, onCircle(center, REFRESH_RADIUS, index / modules.length));
  });
}

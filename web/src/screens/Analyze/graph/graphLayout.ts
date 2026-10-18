import dagre from '@dagrejs/dagre';
import type { XYPosition } from '@xyflow/react';

export interface LayoutBox {
  id: string;
  width: number;
  height: number;
}

export interface LayoutLink {
  source: string;
  target: string;
}

// Vertical gap between layers and horizontal gap between neighbours, in px.
const RANK_GAP = 96;
const NODE_GAP = 64;

/**
 * A layered layout: a module above the modules it depends on, so a layered architecture reads
 * top to bottom (web, service, persistence, model). The thin wrapper keeps dagre out of the rest
 * of the page.
 *
 * @example layeredPositions(boxes, edges).get('web') // { x: 120, y: 0 }
 */
export function layeredPositions(boxes: LayoutBox[], links: LayoutLink[]): Map<string, XYPosition> {
  const graph = new dagre.graphlib.Graph();
  graph.setGraph({ rankdir: 'TB', ranksep: RANK_GAP, nodesep: NODE_GAP, marginx: 0, marginy: 0 });
  graph.setDefaultEdgeLabel(() => ({}));
  boxes.forEach((box) => graph.setNode(box.id, { width: box.width, height: box.height }));
  links.forEach((link) => graph.setEdge(link.source, link.target));
  dagre.layout(graph);
  return new Map(boxes.map((box) => [box.id, topLeftOf(graph.node(box.id), box)]));
}

// dagre places a node by its centre; React Flow by its top-left corner.
function topLeftOf(centre: { x: number; y: number }, box: LayoutBox): XYPosition {
  return { x: Math.round(centre.x - box.width / 2), y: Math.round(centre.y - box.height / 2) };
}

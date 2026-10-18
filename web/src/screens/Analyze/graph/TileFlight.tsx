import { useReactFlow, type InternalNode } from '@xyflow/react';
import { useEffect, useRef } from 'react';
import type { Flight } from '../graphEmphasisContext';
import type { GraphNode } from './graphElements';
import styles from './graph.module.css';

const FLIGHT_MS = 700;

function centreOf(node: InternalNode<GraphNode> | undefined): { x: number; y: number } | null {
  if (!node) return null;
  const { x, y } = node.internals.positionAbsolute;
  return { x: x + (node.measured.width ?? 0) / 2, y: y + (node.measured.height ?? 0) / 2 };
}

function stillMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
}

/**
 * The motion of an applied move: a class tile travels from the module it left to the module it
 * joined. Skipped under prefers-reduced-motion, and wherever the browser cannot animate.
 *
 * @example <TileFlight flight={emphasis.flight} />
 */
export function TileFlight({ flight }: { flight: Flight | null }) {
  const { getInternalNode, flowToScreenPosition } = useReactFlow<GraphNode>();
  const tile = useRef<HTMLSpanElement>(null);
  useEffect(() => {
    const element = tile.current;
    const from = centreOf(flight ? getInternalNode(flight.from) : undefined);
    const to = centreOf(flight ? getInternalNode(flight.to) : undefined);
    if (!element || !from || !to || stillMotion() || typeof element.animate !== 'function') return;
    const [start, end] = [flowToScreenPosition(from), flowToScreenPosition(to)];
    const frames = [start, end].map(({ x, y }) => ({
      transform: `translate(${x - 12}px, ${y - 12}px)`,
      opacity: 1,
    }));
    element.animate([...frames, { ...frames[1], opacity: 0 }], {
      duration: FLIGHT_MS,
      easing: 'ease-in-out',
    });
  }, [flight, getInternalNode, flowToScreenPosition]);
  return <span ref={tile} aria-hidden className={styles.flight} />;
}

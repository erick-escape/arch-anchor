import { BaseEdge, getSmoothStepPath, type EdgeProps } from '@xyflow/react';
import type { DependencyEdge } from './graphElements';
import styles from './graph.module.css';

/**
 * A dependency between modules, or from a module to an external package: solid blue when the
 * contract allows it, dashed red and moving when it is a violation. The arrowhead is drawn here,
 * with a plain id, because React Flow's markers put the colour into the marker's id.
 */
export function DependencyEdgeView(props: EdgeProps<DependencyEdge>) {
  const [path] = getSmoothStepPath({ ...props, borderRadius: 14 });
  const divergence = props.data?.kind === 'divergence';
  const markerId = `aa-arrow-${props.id.replace(/[^A-Za-z0-9_-]/g, '_')}`;
  return (
    <>
      <defs>
        <marker
          id={markerId}
          viewBox="-10 -10 20 20"
          markerWidth="16"
          markerHeight="16"
          orient="auto-start-reverse"
        >
          <polyline
            points="-5,-4 0,0 -5,4 -5,-4"
            className={divergence ? styles.headDivergence : styles.headAllowed}
          />
        </marker>
      </defs>
      <BaseEdge
        id={props.id}
        path={path}
        markerEnd={`url(#${markerId})`}
        className={divergence ? styles.edgeDivergence : styles.edgeAllowed}
      />
    </>
  );
}

import { useNodesState, type OnNodesChange } from '@xyflow/react';
import { useEffect, useMemo, type Dispatch, type SetStateAction } from 'react';
import type { ModuleData } from '../../../interface/ModuleData';
import {
  dependencyEdgesOf,
  nextGraphNodes,
  type DependencyEdge,
  type GraphNode,
} from './graphElements';
import { dependencyGraphOf } from './moduleEdges';

export interface ModuleGraph {
  nodes: GraphNode[];
  edges: DependencyEdge[];
  setNodes: Dispatch<SetStateAction<GraphNode[]>>;
  onNodesChange: OnNodesChange<GraphNode>;
}

/**
 * The graph React Flow draws for the modules on screen. Nodes follow the modules: the same
 * modules keep the positions the user dragged them to; new or removed modules lay it out again.
 *
 * @example const graph = useModuleGraph(analysis.modules, showExternal);
 */
export function useModuleGraph(modules: ModuleData[], showExternal: boolean): ModuleGraph {
  const [nodes, setNodes, onNodesChange] = useNodesState<GraphNode>([]);
  const graph = useMemo(
    () => dependencyGraphOf(modules, { showExternal }),
    [modules, showExternal]
  );
  const edges = useMemo(() => dependencyEdgesOf(graph.edges, modules), [graph, modules]);
  useEffect(() => {
    setNodes((current) => nextGraphNodes(current, modules, graph));
  }, [modules, graph, setNodes]);
  return { nodes, edges, setNodes, onNodesChange };
}

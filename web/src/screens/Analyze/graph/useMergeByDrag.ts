import { useReactFlow, type OnNodeDrag } from '@xyflow/react';
import { useCallback, useState, type Dispatch, type SetStateAction } from 'react';
import type { ModuleData } from '../../../interface/ModuleData';
import { withHighlightedNodes, type GraphNode, type ModuleNode } from './graphElements';

export interface MergeCandidate {
  source: ModuleData;
  target: ModuleData;
}

export interface MergeByDrag {
  candidate: MergeCandidate | null;
  clearCandidate: () => void;
  onNodeDrag: OnNodeDrag<GraphNode>;
  onNodeDragStop: OnNodeDrag<GraphNode>;
}

function isModuleNode(node: GraphNode): node is ModuleNode {
  return node.type === 'module';
}

/**
 * Dropping a module onto another proposes their merge: while dragging, the module under the
 * pointer is highlighted; on drop it becomes the candidate the page asks to confirm.
 *
 * @example const merge = useMergeByDrag(setNodes);
 */
export function useMergeByDrag(setNodes: Dispatch<SetStateAction<GraphNode[]>>): MergeByDrag {
  const { getIntersectingNodes } = useReactFlow<GraphNode>();
  const [candidate, setCandidate] = useState<MergeCandidate | null>(null);
  const targetsUnder = useCallback(
    (dragged: GraphNode) =>
      getIntersectingNodes(dragged).filter((node) => isModuleNode(node) && node.id !== dragged.id),
    [getIntersectingNodes]
  );
  const onNodeDrag = useCallback<OnNodeDrag<GraphNode>>(
    (_event, dragged) =>
      setNodes((nodes) =>
        withHighlightedNodes(
          nodes,
          targetsUnder(dragged).map((node) => node.id)
        )
      ),
    [setNodes, targetsUnder]
  );
  const onNodeDragStop = useCallback<OnNodeDrag<GraphNode>>(
    (_event, dragged) => {
      setNodes((nodes) => withHighlightedNodes(nodes, []));
      const [target] = targetsUnder(dragged).filter(isModuleNode);
      if (target && isModuleNode(dragged))
        setCandidate({ source: dragged.data.module, target: target.data.module });
    },
    [setNodes, targetsUnder]
  );
  return { candidate, clearCandidate: () => setCandidate(null), onNodeDrag, onNodeDragStop };
}

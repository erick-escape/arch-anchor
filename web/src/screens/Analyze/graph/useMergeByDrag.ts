import { useReactFlow, type OnNodeDrag } from '@xyflow/react';
import { useCallback, type Dispatch, type SetStateAction } from 'react';
import type { ModuleData } from '../../../interface/ModuleData';
import { withHighlightedNodes, type GraphNode, type ModuleNode } from './graphElements';

export interface MergeByDrag {
  onNodeDrag: OnNodeDrag<GraphNode>;
  onNodeDragStop: OnNodeDrag<GraphNode>;
}

function isModuleNode(node: GraphNode): node is ModuleNode {
  return node.type === 'module';
}

/**
 * Dropping a module onto another proposes their merge: while dragging, the module under the
 * pointer is highlighted; on drop, `onDropOnto` hears which module landed on which.
 *
 * @example const merge = useMergeByDrag(setNodes, (source, target) => propose({ source, target }));
 */
export function useMergeByDrag(
  setNodes: Dispatch<SetStateAction<GraphNode[]>>,
  onDropOnto: (source: ModuleData, target: ModuleData) => void
): MergeByDrag {
  const { getIntersectingNodes } = useReactFlow<GraphNode>();
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
      if (target && isModuleNode(dragged)) onDropOnto(dragged.data.module, target.data.module);
    },
    [setNodes, targetsUnder, onDropOnto]
  );
  return { onNodeDrag, onNodeDragStop };
}

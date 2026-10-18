import { ReactFlow } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useState } from 'react';
import type { ModuleData } from '../../interface/ModuleData';
import { useTheme } from '../../theme/themeContext';
import styles from './Analyze.module.css';
import { DependencyEdgeView } from './graph/DependencyEdgeView';
import { ModuleNodeView } from './graph/ModuleNodeView';
import { PackageNodeView } from './graph/PackageNodeView';
import { useMergeByDrag } from './graph/useMergeByDrag';
import { useModuleGraph } from './graph/useModuleGraph';
import { GraphLegend, ZoomControls } from './GraphOverlays';

const nodeTypes = { module: ModuleNodeView, package: PackageNodeView };
const edgeTypes = { dependency: DependencyEdgeView };

interface ModuleGraphCanvasProps {
  modules: ModuleData[];
  /** A module was dropped onto another; the page asks whether to merge them. */
  onProposeMerge: (source: ModuleData, target: ModuleData) => void;
}

/**
 * The module graph, full-bleed under the floating panels: modules, the dependencies between
 * them, the external packages of the violations, a legend and zoom controls. Dropping a module
 * onto another proposes their merge.
 */
export function ModuleGraphCanvas({ modules, onProposeMerge }: ModuleGraphCanvasProps) {
  const [showExternal, setShowExternal] = useState(false);
  const { mode } = useTheme();
  const graph = useModuleGraph(modules, showExternal);
  const merge = useMergeByDrag(graph.setNodes, onProposeMerge);
  return (
    <>
      <div className={styles.canvas}>
        <ReactFlow
          nodes={graph.nodes}
          edges={graph.edges}
          onNodesChange={graph.onNodesChange}
          onNodeDrag={merge.onNodeDrag}
          onNodeDragStop={merge.onNodeDragStop}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          colorMode={mode}
          fitView
          fitViewOptions={{ padding: 0.12 }}
          minZoom={0.2}
          proOptions={{ hideAttribution: true }}
        />
      </div>
      <GraphLegend showExternal={showExternal} onShowExternalChange={setShowExternal} />
      <ZoomControls />
    </>
  );
}

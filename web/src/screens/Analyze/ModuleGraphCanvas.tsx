import { ReactFlow } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useState } from 'react';
import { MergeConfirmPopup } from '../../components/Popup/MergeConfirmPopup';
import { useToast } from '../../components/feedback/useToast';
import type { ModuleData } from '../../interface/ModuleData';
import { mergeModules } from '../../services/moduleApi';
import { useTheme } from '../../theme/themeContext';
import styles from './Analyze.module.css';
import { DependencyEdgeView } from './graph/DependencyEdgeView';
import { ModuleNodeView } from './graph/ModuleNodeView';
import { PackageNodeView } from './graph/PackageNodeView';
import { useMergeByDrag } from './graph/useMergeByDrag';
import { useModuleGraph } from './graph/useModuleGraph';
import { GraphLegend, ZoomControls } from './GraphOverlays';
import type { ModuleChanges } from './useModuleChanges';

const nodeTypes = { module: ModuleNodeView, package: PackageNodeView };
const edgeTypes = { dependency: DependencyEdgeView };

interface ModuleGraphCanvasProps {
  modules: ModuleData[];
  changes: ModuleChanges;
}

/**
 * The module graph, full-bleed under the floating panels: modules, the dependencies between
 * them, the external packages of the violations, a legend and zoom controls. Dropping a module
 * onto another proposes their merge.
 */
export function ModuleGraphCanvas({ modules, changes }: ModuleGraphCanvasProps) {
  const [showExternal, setShowExternal] = useState(false);
  const { mode } = useTheme();
  const graph = useModuleGraph(modules, showExternal);
  const merge = useMergeByDrag(graph.setNodes);
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
      {merge.candidate && (
        <MergeConfirmation
          candidate={merge.candidate}
          onDone={merge.clearCandidate}
          changes={changes}
        />
      )}
    </>
  );
}

interface MergeConfirmationProps {
  candidate: { source: ModuleData; target: ModuleData };
  onDone: () => void;
  changes: ModuleChanges;
}

function MergeConfirmation({ candidate, onDone, changes }: MergeConfirmationProps) {
  const toast = useToast();
  const confirm = async () => {
    try {
      const merged = await mergeModules(candidate.source.id, candidate.target.id);
      changes.onMerged(candidate.source.id, candidate.target.id, merged);
    } catch (error) {
      toast.error((error as Error).message);
    }
    onDone();
  };
  return (
    <div className={styles.mergeLayer}>
      <MergeConfirmPopup
        source={candidate.source.name}
        target={candidate.target.name}
        onConfirm={confirm}
        onCancel={onDone}
      />
    </div>
  );
}

import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  addEdge,
  Background,
  BackgroundVariant,
  Controls,
  Edge,
  OnNodeDrag,
  ReactFlow,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
  XYPosition,
} from '@xyflow/react';
import { useQueryClient } from '@tanstack/react-query';
import { ModuleData } from '../../interface/ModuleData';
import { ProjectAnalyses, RefClassConstraint } from '../../interface/ProjectAnalyses';
import { generateAcsPdf } from '../../utils/exportACs';
import '@xyflow/react/dist/style.css';
import axios from 'axios';

import { containerStyle, reactFlowStyle } from './styles.ts';
import CustomNode from './nodeTypes.tsx';
import {
  layoutModulesConcentric,
  ModuleNode,
  rebuildNodesPreservingPositions,
  toModuleNode,
  withHighlightedNodes,
  withoutIds,
  withRenamedModule,
  withRenamedModuleNode,
  withSplitModuleNodes,
} from './moduleNodes.ts';
import { MergeConfirmPopup } from '../../components/Popup/MergeConfirmPopup.tsx';
import Header from '../../components/Header/index.tsx';
import Sidebar from '../../components/Sidebar/index.tsx';
import RecommendationsSidebar from '../../components/RecommendationsSidebar/index.tsx';

// 1) Provide a nodeTypes mapping
const nodeTypes = {
  customNode: CustomNode,
};

// A module dragged onto another one, waiting for the user to confirm the merge.
interface MergeCandidate {
  sourceNode: ModuleNode;
  targetNode: ModuleNode;
}

function viewportCenter(): XYPosition {
  return { x: window.innerWidth / 2, y: window.innerHeight / 2 };
}

// The constraints exported to PDF follow the module's reference classes and their enforce modes.
function refClassConstraintsOf(module: ModuleData): RefClassConstraint[] {
  return module.refClazzes.map((rc) => ({
    refClassId: rc.id,
    refClassName: rc.name,
    enforceMode: rc.enforceMode ?? 'ALLOW',
    dependencies: rc.dependencies,
  }));
}

const AnalyzePage = () => {
  const { projectName } = useParams();
  const [loading, setLoading] = useState(false);
  const [modules, setModules] = useState<ModuleData[]>([]);
  const [projectAnalyses, setProjectAnalyses] = useState<ProjectAnalyses>();
  const [nodes, setNodes, onNodesChange] = useNodesState<ModuleNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const { getIntersectingNodes } = useReactFlow<ModuleNode, Edge>();
  const queryClient = useQueryClient();

  // State for sidebar and layout
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [isRecommendationsSidebarOpen] = useState(true);

  // Ref to track if we've already initiated analysis for this project
  const analysisInitiated = useRef<string | null>(null);

  // State for the merge confirmation popup
  const [mergeCandidate, setMergeCandidate] = useState<MergeCandidate | null>(null);

  const fetchModules = async () => {
    // Prevent duplicate calls using ref (StrictMode protection)
    if (analysisInitiated.current === projectName) {
      console.log(
        '🟡 [FRONTEND] fetchModules() called but analysis already initiated for project:',
        projectName,
        ', skipping...'
      );
      return;
    }

    // Prevent duplicate calls if already loading
    if (loading) {
      console.log('🟡 [FRONTEND] fetchModules() called but already loading, skipping...');
      return;
    }

    // Mark this project as analysis initiated
    analysisInitiated.current = projectName ?? null;

    console.log('🟢 [FRONTEND] fetchModules() START for project:', projectName);
    setLoading(true);
    try {
      const response = await axios.post<ProjectAnalyses>('/api/analyze', null, {
        params: { projectName },
      });
      const projectData = response.data;
      const data = projectData.modulesList;
      setProjectAnalyses(projectData);
      setModules(data);
      setNodes(layoutModulesConcentric(data, viewportCenter()));
      console.log('🟢 [FRONTEND] fetchModules() completed, received', data.length, 'modules');

      // Refetch recommendations after analysis completes
      console.log('🔄 [FRONTEND] Invalidating recommendations query to fetch fresh data');
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    } catch (error) {
      console.error('🔴 [FRONTEND] Failed to analyze project', error);
      // Reset the ref on error so retry is possible
      analysisInitiated.current = null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Add defensive logging to track when useEffect runs
    console.log('🔵 [FRONTEND] useEffect triggered for projectName:', projectName);

    // Reset analysis tracking when project changes
    if (analysisInitiated.current !== projectName) {
      analysisInitiated.current = null;
    }

    if (projectName) {
      fetchModules();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectName]);

  // Toggle sidebar
  const toggleSidebar = () => {
    setIsSidebarOpen(!isSidebarOpen);
  };

  // Handle confirmation from popup
  const handleMergeConfirm = async () => {
    if (!mergeCandidate) return;
    const { sourceNode, targetNode } = mergeCandidate;
    const mergedIds = [sourceNode.id, targetNode.id];

    try {
      // Call the backend to merge modules
      const response = await axios.post<ModuleData>('/api/module/merge', null, {
        params: {
          sourceId: sourceNode.id,
          targetId: targetNode.id,
        },
      });
      const mergedModule = response.data;

      // The merged module takes the target's place
      setNodes((prevNodes) => [
        ...withoutIds(prevNodes, mergedIds),
        toModuleNode(mergedModule, { ...targetNode.position }),
      ]);
      setModules((prevModules) => [...withoutIds(prevModules, mergedIds), mergedModule]);
    } catch (error) {
      console.error('Failed to merge modules', error);
    }
    setMergeCandidate(null);
  };

  // Cancel merge
  const handleMergeCancel = () => {
    setMergeCandidate(null);
  };

  // Apply highlighting during dragging
  const onNodeDrag = useCallback<OnNodeDrag<ModuleNode>>(
    (_event, node) => {
      const intersections = getIntersectingNodes(node).map((n) => n.id);
      setNodes((ns) => withHighlightedNodes(ns, intersections));
    },
    [getIntersectingNodes, setNodes]
  );

  // Handle merge on drag stop
  const onNodeDragStop = useCallback<OnNodeDrag<ModuleNode>>(
    (_event, draggedNode) => {
      const intersections = getIntersectingNodes(draggedNode).filter(
        (n) => n.id !== draggedNode.id // Filter out the node itself
      );

      // Reset highlighting
      setNodes((ns) => withHighlightedNodes(ns, []));

      if (intersections.length > 0) {
        // Take the first intersecting node as the target and ask for confirmation
        setMergeCandidate({ sourceNode: draggedNode, targetNode: intersections[0] });
      }
    },
    [getIntersectingNodes, setNodes]
  );

  const onDeleteRefresh = (deletedModuleId: string) => {
    setModules((prev) => withoutIds(prev, [deletedModuleId]));
    setNodes((prev) => withoutIds(prev, [deletedModuleId]));
  };

  const onRenameRefresh = (moduleId: string, newName: string) => {
    setModules((prev) => withRenamedModule(prev, moduleId, newName));
    setNodes((prev) => withRenamedModuleNode(prev, moduleId, newName));
  };

  const onRefClazzModeRefresh = (moduleId: string, updatedModule: ModuleData) => {
    setModules((prev) => prev.map((m) => (m.id === moduleId ? updatedModule : m)));

    setProjectAnalyses((prev) => {
      if (!prev) return prev;
      const updatedConstraints = prev.architecturalConstraints.map((constraint) =>
        constraint.moduleId === moduleId
          ? { ...constraint, refClassConstraints: refClassConstraintsOf(updatedModule) }
          : constraint
      );
      return { ...prev, architecturalConstraints: updatedConstraints };
    });
  };

  const onSplitRefresh = (oldModuleId: string, newModules: ModuleData[]) => {
    setModules((prev) => [...withoutIds(prev, [oldModuleId]), ...newModules]);
    setNodes((prev) => withSplitModuleNodes(prev, oldModuleId, newModules));
  };

  const handleExportACs = () => {
    if (!projectAnalyses) return;
    const doc = generateAcsPdf(
      projectAnalyses.projectName,
      projectAnalyses.architecturalConstraints
    );
    doc.save(`architectural-constraints-${projectAnalyses.projectName}.pdf`);
  };

  // Refresh all modules by refetching from backend
  const onRecommendationApplied = async () => {
    try {
      console.log('🔄 [FRONTEND] Refreshing modules after recommendation applied');

      // Re-analyze to get updated modules
      const response = await axios.post<ProjectAnalyses>('/api/analyze', null, {
        params: { projectName },
      });
      const projectData = response.data;
      const data = projectData.modulesList;

      console.log('✅ [FRONTEND] Received', data.length, 'modules from backend');

      // Update project and modules state
      setProjectAnalyses(projectData);
      setModules(data);

      // Keep the positions of modules that survived, so the graph does not jump
      setNodes((current) => rebuildNodesPreservingPositions(current, data, viewportCenter()));

      console.log('✅ [FRONTEND] UI updated with new modules');

      // Invalidate recommendations to fetch fresh ones
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    } catch (error) {
      console.error('❌ [FRONTEND] Failed to refresh modules', error);
    }
  };

  return (
    <div
      style={{
        ...containerStyle,
        overflow: 'hidden', // Prevent scrolling when sidebar opens
      }}
    >
      <Header
        projectName={projectName ?? ''}
        isSidebarOpen={isSidebarOpen}
        onToggleSidebar={toggleSidebar}
        onExportACs={handleExportACs}
      />

      <div
        style={{
          position: 'relative',
          width: '100%',
          height: 'calc(100vh - 60px)', // Adjust for header height
          marginLeft: isSidebarOpen ? '300px' : '0',
          marginRight: isRecommendationsSidebarOpen ? '350px' : '0',
          transition: 'margin-left 0.3s ease, margin-right 0.3s ease',
        }}
      >
        {loading ? (
          <p>Loading...</p>
        ) : (
          <>
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={(connection) => setEdges((eds) => addEdge(connection, eds))}
              onNodeDrag={onNodeDrag}
              onNodeDragStop={onNodeDragStop}
              nodeTypes={nodeTypes}
              fitView
              style={{
                ...reactFlowStyle,
                width: '100%',
                height: '100%',
              }}
            >
              <Background variant={BackgroundVariant.Dots} gap={100} size={3} />
              <Controls style={{ color: 'black' }} />
            </ReactFlow>

            {/* Popup layer outside ReactFlow but inside container */}
            <div
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                pointerEvents: 'none',
                zIndex: 9000,
              }}
            >
              {mergeCandidate && (
                <MergeConfirmPopup
                  source={mergeCandidate.sourceNode.data.module.name}
                  target={mergeCandidate.targetNode.data.module.name}
                  onConfirm={handleMergeConfirm}
                  onCancel={handleMergeCancel}
                />
              )}
            </div>
          </>
        )}
      </div>

      <Sidebar
        isOpen={isSidebarOpen}
        modules={modules}
        onDeleteRefresh={onDeleteRefresh}
        onRenameRefresh={onRenameRefresh}
        onSplitRefresh={onSplitRefresh}
        onRefClazzModeRefresh={onRefClazzModeRefresh}
      />

      <RecommendationsSidebar onRecommendationApplied={onRecommendationApplied} />
    </div>
  );
};

function AnalyzePageWithProvider() {
  return (
    <ReactFlowProvider>
      <AnalyzePage />
    </ReactFlowProvider>
  );
}

export default AnalyzePageWithProvider;

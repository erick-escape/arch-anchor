import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { addEdge, Background, Controls, ReactFlow, ReactFlowProvider, useEdgesState, useNodesState, useReactFlow } from '@xyflow/react';
import { useQueryClient } from '@tanstack/react-query';
import { ModuleData } from '../../interface/ModuleData';
import { ProjectAnalyses } from '../../interface/ProjectAnalyses';
import { generateAcsPdf } from '../../utils/exportACs';
import '@xyflow/react/dist/style.css';
import axios from 'axios';

import { containerStyle, reactFlowStyle } from './styles.ts';
import CustomNode from './nodeTypes.tsx';
import { MergeConfirmPopup } from '../../components/Popup/MergeConfirmPopup.tsx';
import Header from '../../components/Header/index.tsx';
import Sidebar from '../../components/Sidebar/index.tsx';
import RecommendationsSidebar from '../../components/RecommendationsSidebar/index.tsx';

// 1) Provide a nodeTypes mapping
const nodeTypes = {
    customNode: CustomNode
};

const AnalyzePage = () => {
    const { projectName } = useParams();
    const [loading, setLoading] = useState(false);
    const [modules, setModules] = useState<ModuleData[]>();
    const [projectAnalyses, setProjectAnalyses] = useState<ProjectAnalyses>();
    const [nodes, setNodes, onNodesChange] = useNodesState([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState([]);
    const { getIntersectingNodes } = useReactFlow();
    const queryClient = useQueryClient();

    // State for sidebar and layout
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [isRecommendationsSidebarOpen] = useState(true);

    // Ref to track if we've already initiated analysis for this project
    const analysisInitiated = useRef<string | null>(null);

    // State for the merge confirmation popup
    const [mergePopup, setMergePopup] = useState({
        show: false,
        sourceNode: null,
        targetNode: null
    });

    const showNodes = (data: ModuleData[]) => {
        // Calculate positions using a concentric circle layout:
        const centerX = window.innerWidth / 2;
        const centerY = window.innerHeight / 2;
        const nodesArray = [];

        if (data.length === 0) {
            // no nodes to display
        } else if (data.length === 1) {
            // Only one node: put it in the center.
            nodesArray.push({
                id: data[0].id,
                type: 'customNode',
                data: { module: data[0] },
                position: { x: centerX, y: centerY },
                draggable: true
            });
        } else {
            // Place the first node in the center.
            nodesArray.push({
                id: data[0].id,
                name: data[0].name,
                type: 'customNode',
                data: { module: data[0] },
                position: { x: centerX, y: centerY },
                draggable: true
            });

            // Now, arrange remaining nodes in concentric rings.
            const ringGap = 200; // gap between rings (adjust as needed)
            let index = 1; // already placed the first node
            let ring = 1;

            while (index < data.length) {
                const ringRadius = ring * ringGap;
                // Estimate capacity for current ring:
                // Assume average node width of 150px => capacity = floor(circumference / 150)
                const capacity = Math.max(Math.floor((2 * Math.PI * ringRadius) / 150), 1);
                for (let i = 0; i < capacity && index < data.length; i++, index++) {
                    const angle = (2 * Math.PI * i) / capacity;
                    const x = centerX + ringRadius * Math.cos(angle);
                    const y = centerY + ringRadius * Math.sin(angle);
                    nodesArray.push({
                        id: data[index].id,
                        name: data[index].name,
                        type: 'customNode',
                        data: { module: data[index] },
                        position: { x, y },
                        draggable: true
                    });
                }
                ring++;
            }
        }

        setNodes(nodesArray);
    };

    const fetchModules = async () => {
        // Prevent duplicate calls using ref (StrictMode protection)
        if (analysisInitiated.current === projectName) {
            console.log('🟡 [FRONTEND] fetchModules() called but analysis already initiated for project:', projectName, ', skipping...');
            return;
        }
        
        // Prevent duplicate calls if already loading
        if (loading) {
            console.log('🟡 [FRONTEND] fetchModules() called but already loading, skipping...');
            return;
        }
        
        // Mark this project as analysis initiated
        analysisInitiated.current = projectName;
        
        console.log('🟢 [FRONTEND] fetchModules() START for project:', projectName);
        setLoading(true);
        try {
            const response = await axios.post<ProjectAnalyses>('/api/analyze', null, {
                params: { projectName }
            });
            const projectData = response.data;
            const data = projectData.modulesList;
            setProjectAnalyses(projectData);
            setModules(data);
            // For each module, create a node with type 'customNode'
            //    and pass the module object via data: { module: mod }
            showNodes(data);
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
    }, [projectName]);

    // Toggle sidebar
    const toggleSidebar = () => {
        setIsSidebarOpen(!isSidebarOpen);
    };

    // Handle confirmation from popup
    const handleMergeConfirm = async () => {
        if (!mergePopup.sourceNode || !mergePopup.targetNode) return;

        const sourceModuleId = mergePopup.sourceNode.id;
        const targetModuleId = mergePopup.targetNode.id;

        // Store the target node's position before merging
        const targetPosition = {
            x: mergePopup.targetNode.position.x,
            y: mergePopup.targetNode.position.y
        };

        try {
            // Call the backend to merge modules
            const response = await axios.post('/api/module/merge', null, {
                params: {
                    sourceId: sourceModuleId,
                    targetId: targetModuleId
                }
            });

            // Get the merged module from the response
            const mergedModule = response.data;

            // Create a new node for the merged module at the target's position
            const mergedNode = {
                id: mergedModule.id,
                name: mergedModule.name,
                type: 'customNode',
                data: { module: mergedModule },
                position: targetPosition,
                draggable: true
            };

            // Update the nodes state by filtering out the source and target nodes
            // and adding the new merged node
            setNodes(prevNodes =>
                prevNodes
                    .filter(node => node.id !== sourceModuleId && node.id !== targetModuleId)
                    .concat(mergedNode)
            );

            // Update modules state to reflect changes
            setModules(prevModules => {
                const updatedModules = prevModules.filter(
                    mod => mod.id !== sourceModuleId && mod.id !== targetModuleId
                );
                return [...updatedModules, mergedModule];
            });

            // Hide the popup
            setMergePopup({ show: false, sourceNode: null, targetNode: null });
        } catch (error) {
            console.error('Failed to merge modules', error);
            setMergePopup({ show: false, sourceNode: null, targetNode: null });
        }
    };

    // Cancel merge
    const handleMergeCancel = () => {
        setMergePopup({ show: false, sourceNode: null, targetNode: null });
    };

    // Apply highlighting during dragging
    const onNodeDrag = useCallback((event, node) => {
        const intersections = getIntersectingNodes(node).map((n) => n.id);

        setNodes((ns) =>
            ns.map((n) => ({
                ...n,
                className: intersections.includes(n.id) ? 'highlight' : ''
            }))
        );
    }, [getIntersectingNodes, setNodes]);

    // Handle merge on drag stop
    const onNodeDragStop = useCallback((event, draggedNode) => {
        const intersections = getIntersectingNodes(draggedNode).filter(
            (n) => n.id !== draggedNode.id // Filter out the node itself
        );

        // Reset highlighting
        setNodes((ns) =>
            ns.map((n) => ({
                ...n,
                className: ''
            }))
        );

        if (intersections.length > 0) {
            // Take the first intersecting node as the target
            const targetNode = intersections[0];
            // Show the confirmation popup
            setMergePopup({
                show: true,
                sourceNode: draggedNode,
                targetNode: targetNode
            });
        }
    }, [getIntersectingNodes, setNodes]);

    const onDeleteRefresh = (deletedModuleId: string) => {
        // Remove from sidebar data
        setModules((prev) => prev.filter((m: ModuleData) => m.id !== deletedModuleId));
        // Remove from React Flow nodes
        setNodes((prev) => prev.filter((n) => n.id !== deletedModuleId));
    };

    const onRenameRefresh = (moduleId: string, newName: string) => {
        // Update in sidebar data
        setModules((prev) => prev.map((module) => {
            if (module.id === moduleId) {
                module.name = newName;
            }
            return module;
        }));
        // Update in React Flow nodes
        setNodes((prev) => prev.map((node) => {
            if (node.id === moduleId) {
                node.name = newName;
            }
            return node;
        }));
    };

    const onSplitRefresh = (oldModuleId: string, newModules: ModuleData[]) => {
        // Find original node position
        const oldNode = nodes.find((n) => n.id === oldModuleId);
        const basePos = oldNode?.position || { x: 0, y: 0 };
        const offset = 100;
        // Build new nodes side by side
        const splitNodes = newModules.map((mod, idx) => ({
            id: mod.id,
            name: mod.name,
            type: 'customNode',
            position: { x: basePos.x + (idx === 0 ? -offset : offset), y: basePos.y },
            data: { module: mod }
        }));

        // Update sidebar data: remove old, add new
        setModules((prev) => {
            const filtered = prev.filter((m) => m.id !== oldModuleId);
            return [...filtered, ...newModules];
        });
        // Update nodes: remove old, add new
        setNodes((prev) => {
            const filtered = prev.filter((n) => n.id !== oldModuleId);
            return [...filtered, ...splitNodes];
        });
    };

    // Smart update that preserves node positions when possible
    const updateNodesSmartly = (newModules: ModuleData[]) => {
        setNodes((currentNodes) => {
            // Create a map of existing positions
            const positionMap = new Map();
            currentNodes.forEach(node => {
                positionMap.set(node.id, node.position);
            });

            // Get current module IDs
            const currentModuleIds = new Set(currentNodes.map(n => n.id));
            const newModuleIds = new Set(newModules.map(m => m.id));

            // Check if there are any changes
            const hasChanges =
                currentModuleIds.size !== newModuleIds.size ||
                [...currentModuleIds].some(id => !newModuleIds.has(id)) ||
                [...newModuleIds].some(id => !currentModuleIds.has(id));

            if (!hasChanges) {
                // No structural changes, just update data
                console.log('📝 [FRONTEND] No structural changes, updating node data only');
                return currentNodes.map(node => {
                    const moduleData = newModules.find(m => m.id === node.id);
                    return moduleData ? {
                        ...node,
                        data: { module: moduleData }
                    } : node;
                });
            }

            console.log('🔄 [FRONTEND] Structural changes detected, rebuilding nodes');

            // Calculate center for new nodes
            const centerX = window.innerWidth / 2;
            const centerY = window.innerHeight / 2;

            // Build new nodes array, preserving positions where possible
            const newNodes = newModules.map((module, index) => {
                const existingPosition = positionMap.get(module.id);

                if (existingPosition) {
                    // Keep existing position
                    return {
                        id: module.id,
                        name: module.name,
                        type: 'customNode',
                        data: { module },
                        position: existingPosition,
                        draggable: true
                    };
                } else {
                    // New module - place in a new position
                    const angle = (2 * Math.PI * index) / newModules.length;
                    const radius = 300;
                    return {
                        id: module.id,
                        name: module.name,
                        type: 'customNode',
                        data: { module },
                        position: {
                            x: centerX + radius * Math.cos(angle),
                            y: centerY + radius * Math.sin(angle)
                        },
                        draggable: true
                    };
                }
            });

            return newNodes;
        });
    };

    const handleExportACs = () => {
        if (!projectAnalyses) return;
        const doc = generateAcsPdf(projectAnalyses.projectName, projectAnalyses.architecturalConstraints);
        doc.save(`architectural-constraints-${projectAnalyses.projectName}.pdf`);
    };

    // Refresh all modules by refetching from backend
    const onRecommendationApplied = async () => {
        try {
            console.log('🔄 [FRONTEND] Refreshing modules after recommendation applied');

            // Re-analyze to get updated modules
            const response = await axios.post<ProjectAnalyses>('/api/analyze', null, {
                params: { projectName }
            });
            const projectData = response.data;
            const data = projectData.modulesList;

            console.log('✅ [FRONTEND] Received', data.length, 'modules from backend');

            // Update project and modules state
            setProjectAnalyses(projectData);
            setModules(data);

            // Smart update that preserves positions
            updateNodesSmartly(data);

            console.log('✅ [FRONTEND] UI updated with new modules');

            // Invalidate recommendations to fetch fresh ones
            queryClient.invalidateQueries({ queryKey: ['recommendations'] });
        } catch (error) {
            console.error('❌ [FRONTEND] Failed to refresh modules', error);
        }
    };

    return (
        <div style={{
            ...containerStyle,
            overflow: 'hidden' // Prevent scrolling when sidebar opens
        }}>
            <Header
                projectName={projectName}
                isSidebarOpen={isSidebarOpen}
                onToggleSidebar={toggleSidebar}
                onExportACs={handleExportACs}
            />

            <div style={{
                position: 'relative',
                width: '100%',
                height: 'calc(100vh - 60px)', // Adjust for header height
                marginLeft: isSidebarOpen ? '300px' : '0',
                marginRight: isRecommendationsSidebarOpen ? '350px' : '0',
                transition: 'margin-left 0.3s ease, margin-right 0.3s ease'
            }}>
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
                                height: '100%'
                            }}
                        >
                            <Background variant="dots" gap={100} size={3} />
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
                                zIndex: 9000
                            }}
                        >
                            {mergePopup.show && mergePopup.sourceNode && mergePopup.targetNode && (
                                <MergeConfirmPopup
                                    source={mergePopup.sourceNode.name}
                                    target={mergePopup.targetNode.name}
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
import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { addEdge, Background, Controls, ReactFlow, ReactFlowProvider, useEdgesState, useNodesState } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import axios from 'axios';

import { containerStyle, reactFlowStyle } from './styles.ts';
import CustomNode from './nodeTypes.tsx';

// 1) Provide a nodeTypes mapping
const nodeTypes = {
    customNode: CustomNode
};

const AnalyzePage = () => {
    const { projectName } = useParams();
    const [loading, setLoading] = useState(false);
    const [modules, setModules] = useState([]);
    const [nodes, setNodes, onNodesChange] = useNodesState([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState([]);

    useEffect(() => {
        const fetchAnalyzedModules = async () => {
            setLoading(true);
            try {
                const response = await axios.post('/api/analyze', null, {
                    params: { projectName }
                });

                const data = response.data;
                setModules(data);

                // 2) For each module, create a node with type 'customNode'
                //    and pass the module object via data: { module: mod }
                // Calculate positions using a concentric circle layout:
                const centerX = window.innerWidth / 2;
                const centerY = window.innerHeight / 2;
                const nodesArray = [];

                if (data.length === 0) {
                    // no nodes to display
                } else if (data.length === 1) {
                    // Only one node: put it in the center.
                    nodesArray.push({
                        id: data[0].name,
                        type: 'customNode',
                        data: { module: data[0] },
                        position: { x: centerX, y: centerY }
                    });
                } else {
                    // Place the first node in the center.
                    nodesArray.push({
                        id: data[0].name,
                        type: 'customNode',
                        data: { module: data[0] },
                        position: { x: centerX, y: centerY }
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
                                id: data[index].name,
                                type: 'customNode',
                                data: { module: data[index] },
                                position: { x, y }
                            });
                        }
                        ring++;
                    }
                }

                setNodes(nodesArray);
            } catch (error) {
                console.error('Failed to analyze project', error);
            } finally {
                setLoading(false);
            }
        };
        fetchAnalyzedModules();
    }, [projectName, setNodes]);

    // For grouping
    const onDrop = useCallback((event) => {
        event.preventDefault();
        const { source, target } = JSON.parse(
            event.dataTransfer.getData('application/reactflow')
        );
        axios.post('/api/module/join', { parent: target, child: source })
            .then(() => {
                console.log('Modules joined');
            })
            .catch((err) => console.error('Failed to join modules', err));
    }, []);

    const handleDelete = (nodeId) => {
        setNodes((nds) => nds.filter((node) => node.id !== nodeId));
    };

    return (
        <div style={containerStyle}>
            <h3>Analyzed Modules for Project: {projectName}</h3>
            {loading ? (
                <p>Loading...</p>
            ) : (
                <ReactFlow
                    nodes={nodes}
                    edges={edges}
                    onNodesChange={onNodesChange}
                    onEdgesChange={onEdgesChange}
                    onConnect={(connection) => setEdges((eds) => addEdge(connection, eds))}
                    onDrop={onDrop}
                    nodeTypes={nodeTypes} // 3) Provide nodeTypes to ReactFlow
                    fitView
                    style={reactFlowStyle}
                >
                    <Background variant="dots" gap={100} size={3} />
                    <Controls style={{ color: 'black' }} />
                    {/*<MiniMap/>*/}
                </ReactFlow>
            )}
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

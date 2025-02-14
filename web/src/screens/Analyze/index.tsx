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
                setNodes(
                    data.map((mod, index) => ({
                        id: mod.name,
                        type: 'customNode',
                        data: { module: mod }, // pass the actual data
                        position: { x: index * 200, y: index } // AQUI QUE VOU MUDAR PARA AJUSTAR A POSIÇÃO INICIAL
                    }))
                );
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

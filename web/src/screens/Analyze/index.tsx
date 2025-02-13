import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { addEdge, Background, Controls, ReactFlow, useEdgesState, useNodesState } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import axios from 'axios';
import { containerStyle, nodeStyle, reactFlowStyle } from './styles.ts';

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
                setModules(response.data);
                setNodes(response.data.map((mod, index) => ({
                    id: mod.name,
                    data: { label: renderModule(mod) },
                    position: { x: index * 200, y: 100 }
                })));
            } catch (error) {
                console.error('Failed to analyze project', error);
            } finally {
                setLoading(false);
            }
        };
        fetchAnalyzedModules();
    }, [projectName]);

    const renderModule = (mod) => (
        <div style={nodeStyle}>
            {/* Title: center-aligned */}
            <div style={{ width: '100%', textAlign: 'center', fontWeight: 'bold' }}>
                {mod.name}
            </div>

            {/* Ref Class: same line as label */}
            <div>
                <strong>Ref Class:</strong> {mod.refClass || 'N/A'}
            </div>

            {/* Similarity: same line as label */}
            <div>
                <strong>Similarity:</strong> {(mod.similarity * 100).toFixed(2)}%
            </div>
        </div>
    );


    const onDrop = useCallback((event) => {
        event.preventDefault();
        const { source, target } = JSON.parse(event.dataTransfer.getData('application/reactflow'));
        axios.post('/api/module/join', { parent: target, child: source })
            .then(() => {
                console.log('Modules joined');
            })
            .catch(err => console.error('Failed to join modules', err));
    }, []);

    const handleDelete = (nodeId) => {
        setNodes(nodes.filter(node => node.id !== nodeId));
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
                    fitView
                    style={reactFlowStyle}
                >
                    <Background variant="dots" gap={100} size={3} />
                    <Controls style={{ color: 'black' }} />
                    {/*<MiniMap />*/}
                </ReactFlow>
            )}
        </div>
    );
};

export default AnalyzePage;

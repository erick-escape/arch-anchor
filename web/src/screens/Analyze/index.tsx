import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Card, message, Spin } from 'antd';
import axios from 'axios';

const AnalyzePage = () => {
    const { projectName } = useParams();
    const [modules, setModules] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchAnalyzedModules = async () => {
            setLoading(true);
            try {
                const response = await axios.post('/api/analyze', null, {
                    params: { projectName }
                });
                setModules(response.data);
            } catch (error) {
                message.error('Failed to analyze project');
            } finally {
                setLoading(false);
            }
        };

        fetchAnalyzedModules();
    }, [projectName]);

    return (
        <div style={{ padding: '20px', display: 'flex', flexWrap: 'wrap', gap: '20px' }}>
            <h1>Analyzed Modules for Project: {projectName}</h1>
            {loading ? (
                <Spin size="large" />
            ) : (
                modules.map((module) => (
                    <Card key={module.name} title={module.name} style={{ width: 300 }}>
                        <p>
                            <strong>Ref Class:</strong> {module.refClass || 'N/A'}
                        </p>
                        <p>
                            <strong>Similarity:</strong> {(module.similarity * 100).toFixed(2) + '%'}
                        </p>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '20px' }}>
                            <Button type="primary">Action 1</Button>
                            <Button type="primary">Action 2</Button>
                        </div>
                    </Card>
                ))
            )}
        </div>
    );
};

export default AnalyzePage;

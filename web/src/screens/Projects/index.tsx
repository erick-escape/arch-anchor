import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, message, Popconfirm, Spin, Table } from 'antd';
import UploadModal from '../../components/UploadModal';
import axios from 'axios';
import { ProjectSummary, ProjectsListResponse } from '../../interface/Project';

const ProjectsPage = () => {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploadModalVisible, setUploadModalVisible] = useState(false);

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const response = await axios.get<ProjectsListResponse>('/api/projects');
      setProjects(response.data.projects);
    } catch {
      message.error('Failed to fetch projects');
    } finally {
      setLoading(false);
    }
  };

  const analyzeProject = async (projectName: string) => {
    navigate(`/analyze/${projectName}`);
  };

  const deleteProject = async (projectName: string) => {
    setLoading(true);
    try {
      await axios.delete(`/api/projects/${projectName}`);
      message.success(`Project "${projectName}" deleted successfully!`);
      fetchProjects(); // Refresh project list after deletion
    } catch {
      message.error('Failed to delete project');
    } finally {
      setLoading(false);
    }
  };

  const openUploadModal = () => setUploadModalVisible(true);
  const closeUploadModal = () => setUploadModalVisible(false);

  const handleUploadSuccess = () => {
    closeUploadModal();
    fetchProjects();
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  return (
    <div
      style={{
        padding: 40,
        minHeight: '80vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 20,
          width: '100%',
        }}
      >
        <h1>Projects</h1>
        <Button type="primary" onClick={openUploadModal}>
          Add Project
        </Button>
      </div>

      {loading ? (
        <Spin size="large" />
      ) : (
        <Table
          dataSource={projects}
          rowKey={(record) => record.name} // Use project name as unique key
          style={{ width: '100%' }} // Adjusts the table width
          columns={[
            {
              title: 'Project Name',
              dataIndex: 'name',
              key: 'name',
            },
            {
              title: 'Actions',
              key: 'actions',
              render: (_, record) => (
                <div style={{ display: 'flex', gap: 8 }}>
                  <Button type="primary" onClick={() => analyzeProject(record.name)}>
                    Analyze
                  </Button>
                  <Popconfirm
                    title={`Are you sure you want to delete ${record.name}?`}
                    onConfirm={() => deleteProject(record.name)}
                    okText="Yes"
                    cancelText="No"
                  >
                    <Button type="primary" danger>
                      Delete
                    </Button>
                  </Popconfirm>
                </div>
              ),
            },
          ]}
        />
      )}

      <UploadModal
        open={uploadModalVisible}
        onClose={closeUploadModal}
        onSuccess={handleUploadSuccess}
      />
    </div>
  );
};

export default ProjectsPage;

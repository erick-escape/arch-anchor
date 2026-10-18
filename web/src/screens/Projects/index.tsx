import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Spin } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AddProjectModal from '../../components/AddProjectModal';
import shell from '../../components/AppShell/AppShell.module.css';
import { listProjects } from '../../services/projectsApi';
import { DeleteProjectDialog } from './DeleteProjectDialog';
import { ProjectCard } from './ProjectCard';
import styles from './Projects.module.css';
import { NoProjects, ProjectsUnavailable } from './ProjectsStates';

/**
 * The imported projects as cards: open one for analysis, delete one, or add a project.
 *
 * @example <Route path="/projects" element={<ProjectsPage />} />
 */
const ProjectsPage = () => {
  const [adding, setAdding] = useState(false);
  const queryClient = useQueryClient();
  const closeAdd = () => setAdding(false);
  const added = () => {
    closeAdd();
    void queryClient.invalidateQueries({ queryKey: ['projects'] });
  };
  return (
    <div className={shell.page}>
      <div className={styles.titleRow}>
        <h1 className={shell.heading}>Projects</h1>
        <Button type="primary" onClick={() => setAdding(true)}>
          Add a project
        </Button>
      </div>
      <ProjectList onAdd={() => setAdding(true)} />
      <AddProjectModal open={adding} onClose={closeAdd} onSuccess={added} />
    </div>
  );
};

function ProjectList({ onAdd }: { onAdd: () => void }) {
  const navigate = useNavigate();
  const [toDelete, setToDelete] = useState<string | null>(null);
  const projects = useQuery({ queryKey: ['projects'], queryFn: listProjects });
  if (projects.isPending) return <Spin size="large" className={styles.loading} />;
  if (projects.isError) {
    return (
      <ProjectsUnavailable reason={projects.error.message} onRetry={() => projects.refetch()} />
    );
  }
  if (projects.data.length === 0) return <NoProjects onAdd={onAdd} />;
  return (
    <div className={styles.grid}>
      {projects.data.map(({ name }) => (
        <ProjectCard
          key={name}
          name={name}
          onAnalyze={() => navigate(`/analyze/${encodeURIComponent(name)}`)}
          onDelete={() => setToDelete(name)}
        />
      ))}
      <DeleteProjectDialog name={toDelete} onClose={() => setToDelete(null)} />
    </div>
  );
}

export default ProjectsPage;

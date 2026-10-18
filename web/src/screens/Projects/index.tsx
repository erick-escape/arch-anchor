import { useQuery } from '@tanstack/react-query';
import { Button, Spin } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import shell from '../../components/AppShell/AppShell.module.css';
import { listProjects } from '../../services/projectsApi';
import { DeleteProjectDialog } from './DeleteProjectDialog';
import { ProjectCard } from './ProjectCard';
import styles from './Projects.module.css';
import { NoProjects, ProjectsUnavailable } from './ProjectsStates';

/**
 * The imported projects as cards: open one for analysis or delete one. A project is added from
 * the homepage's import bar, which replaced the Add Project dialog.
 *
 * @example <Route path="/projects" element={<ProjectsPage />} />
 */
const ProjectsPage = () => {
  const navigate = useNavigate();
  const addProject = () => navigate('/');
  return (
    <div className={shell.page}>
      <div className={styles.titleRow}>
        <h1 className={shell.heading}>Projects</h1>
        <Button type="primary" onClick={addProject}>
          Add a project
        </Button>
      </div>
      <ProjectList onAdd={addProject} />
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
      {projects.data.map(({ name, source }) => (
        <ProjectCard
          key={name}
          name={name}
          source={source}
          onAnalyze={() => navigate(`/analyze/${encodeURIComponent(name)}`)}
          onDelete={() => setToDelete(name)}
        />
      ))}
      <DeleteProjectDialog name={toDelete} onClose={() => setToDelete(null)} />
    </div>
  );
}

export default ProjectsPage;

import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listProjects } from '../../services/projectsApi';
import styles from './Home.module.css';

// Enough to come back to recent work; the rest is one click away under All projects.
const SHOWN = 6;

/**
 * The projects already imported, each a click from its analysis. Absent until there is one:
 * a first visit sees the import bar and nothing else to choose from.
 *
 * @example <RecentProjects />
 */
export function RecentProjects() {
  const projects = useQuery({ queryKey: ['projects'], queryFn: listProjects });
  if (!projects.data?.length) return null;
  return (
    <section aria-labelledby="recent-title" className={styles.recent}>
      <div className={styles.recentHead}>
        <h2 id="recent-title" className={styles.recentTitle}>
          Your projects
        </h2>
        <Link to="/projects" className={styles.textLink}>
          All projects
        </Link>
      </div>
      <div className={styles.recentGrid}>
        {projects.data.slice(0, SHOWN).map(({ name }) => (
          <Link
            key={name}
            to={`/analyze/${encodeURIComponent(name)}`}
            className={styles.recentCard}
          >
            <span aria-hidden className={styles.recentMark}>
              <span />
              <span />
              <span />
              <span />
            </span>
            <span className={styles.recentName}>{name}</span>
            <span className={styles.recentAction}>Analyze</span>
          </Link>
        ))}
      </div>
    </section>
  );
}

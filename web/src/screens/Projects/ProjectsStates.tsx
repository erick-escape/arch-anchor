import { Button } from 'antd';
import styles from './Projects.module.css';

/**
 * No project imported yet: what will appear here and how to add the first one.
 *
 * @example <NoProjects onAdd={openImport} />
 */
export function NoProjects({ onAdd }: { onAdd: () => void }) {
  return (
    <div className={styles.empty}>
      <div aria-hidden className={styles.emptyModule}>
        <span className={styles.emptyTab} />
      </div>
      <h2 className={styles.emptyTitle}>No projects yet</h2>
      <p className={styles.emptyText}>
        Import a GitHub repository or upload a folder, and its modules appear here. The bookstore
        example takes one click.
      </p>
      <Button type="primary" size="large" onClick={onAdd}>
        Add a project
      </Button>
    </div>
  );
}

/**
 * The list could not be loaded: why, and a way to try again.
 *
 * @example <ProjectsUnavailable reason={error.message} onRetry={refetch} />
 */
export function ProjectsUnavailable({ reason, onRetry }: { reason: string; onRetry: () => void }) {
  return (
    <div role="alert" className={styles.failure}>
      <span className={styles.failureText}>{reason}</span>
      <Button onClick={onRetry}>Try again</Button>
    </div>
  );
}

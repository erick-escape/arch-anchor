import { Button } from 'antd';
import styles from './Projects.module.css';

interface ProjectCardProps {
  name: string;
  onAnalyze: () => void;
  onDelete: () => void;
}

/**
 * One imported project. Analyze is the card's primary action; deleting asks first.
 *
 * @example <ProjectCard name="bookstore" onAnalyze={open} onDelete={confirmDelete} />
 */
export function ProjectCard({ name, onAnalyze, onDelete }: ProjectCardProps) {
  return (
    <article className={styles.card} aria-labelledby={`project-${name}`}>
      <ModuleSilhouette />
      <h2 id={`project-${name}`} className={styles.cardName}>
        {name}
      </h2>
      <div className={styles.cardActions}>
        <Button type="primary" onClick={onAnalyze}>
          Analyze
        </Button>
        <Button type="text" onClick={onDelete} className={styles.quiet}>
          Delete…
        </Button>
      </div>
    </article>
  );
}

// Decorative: four module containers, one tile each in the reference-class blue.
function ModuleSilhouette() {
  return (
    <div aria-hidden className={styles.silhouette}>
      {[0, 1, 2, 3].map((module) => (
        <span key={module} className={styles.silhouetteModule}>
          <span className={styles.silhouetteReference} />
          <span className={styles.silhouetteTile} />
          <span className={styles.silhouetteTile} />
        </span>
      ))}
    </div>
  );
}

import { Button } from 'antd';
import { Link } from 'react-router-dom';
import styles from './Analyze.module.css';

/**
 * While the API analyzes: one class tile among four turns into the reference class, the
 * election the analysis is running. The API reports no progress, so neither does this.
 */
export function AnalyzingState({ projectName }: { projectName: string }) {
  return (
    <div role="status" aria-live="polite" className={styles.state}>
      <div aria-hidden className={styles.election}>
        <span className={styles.electionTile} />
        <span className={`${styles.electionTile} ${styles.electionWinner}`} />
        <span className={styles.electionTile} />
        <span className={styles.electionTile} />
      </div>
      <h1 className={styles.stateTitle}>Analyzing {projectName}</h1>
      <p className={styles.stateText}>
        Parsing the sources, electing a reference class in every module, then running the split,
        merge, move and violation analyses.
      </p>
    </div>
  );
}

interface AnalysisFailedProps {
  projectName: string;
  reason: string;
  onRetry: () => void;
}

/** The analysis did not finish: why, and what to do. It used to leave an empty canvas. */
export function AnalysisFailed({ projectName, reason, onRetry }: AnalysisFailedProps) {
  return (
    <div className={styles.state}>
      <div role="alert" className={styles.failure}>
        <h1 className={styles.stateTitle}>The analysis of {projectName} did not finish</h1>
        <p className={styles.stateText}>{reason}</p>
        <div className={styles.failureActions}>
          <Button type="primary" onClick={onRetry}>
            Try again
          </Button>
          <Link to="/projects">Back to projects</Link>
        </div>
      </div>
    </div>
  );
}

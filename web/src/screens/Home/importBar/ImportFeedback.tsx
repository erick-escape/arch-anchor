import { faCircleExclamation } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import styles from './ImportBar.module.css';

/**
 * The import under way. The API reports no progress while it clones, so this says only which
 * step is running.
 *
 * @example <ImportProgress what="erick-escape/arch-anchor" />
 */
export function ImportProgress({ what }: { what: string }) {
  return (
    <div role="status" className={styles.progress}>
      <div className={styles.scanTrack}>
        <div className={styles.scan} />
      </div>
      <div className={styles.steps}>
        <span className={styles.stepCurrent}>• Importing {what}</span>
        <span className={styles.stepNext}>• Analyzing it</span>
      </div>
      <span className={styles.note}>
        A large repository takes a while to clone. The import has no progress to report, so this
        shows only which step is running.
      </span>
    </div>
  );
}

/**
 * Why the import failed, in the API's words when it gave any.
 *
 * @example <ImportFailure reason="No src/ folder in owner/repo." />
 */
export function ImportFailure({ reason }: { reason: string }) {
  return (
    <div role="alert" className={styles.failure}>
      <FontAwesomeIcon icon={faCircleExclamation} className={styles.failureIcon} />
      <span>{reason}</span>
    </div>
  );
}

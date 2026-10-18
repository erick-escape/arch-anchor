import { faFolderOpen } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import styles from './Home.module.css';

/**
 * Where a dragged folder will go: over the whole page while the drag is on it.
 *
 * @example {dragging && <DropOverlay />}
 */
export function DropOverlay() {
  return (
    <div className={styles.dropScrim}>
      <div className={styles.dropCard}>
        <FontAwesomeIcon icon={faFolderOpen} className={styles.dropIcon} />
        <span className={styles.dropTitle}>Drop the project folder</span>
        <span className={styles.dropText}>It is uploaded and analyzed straight away.</span>
      </div>
    </div>
  );
}

import type { ReactNode } from 'react';
import styles from './vocabulary.module.css';

interface StatTileProps {
  value: ReactNode;
  caption: string;
  /** "divergence" only for a count of violations. */
  tone?: 'accent' | 'divergence';
}

/**
 * A headline number with its caption.
 *
 * @example <StatTile value="93.75%" caption="project similarity" />
 */
export function StatTile({ value, caption, tone = 'accent' }: StatTileProps) {
  return (
    <div
      className={tone === 'divergence' ? `${styles.stat} ${styles.statDivergence}` : styles.stat}
    >
      <span className={styles.statValue}>{value}</span>
      <span className={styles.statCaption}>{caption}</span>
    </div>
  );
}

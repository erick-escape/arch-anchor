import { formatRate } from '../../utils/formatPercent';
import type { PriorityLabel } from '../../utils/priority';
import styles from './vocabulary.module.css';

interface PriorityBadgeProps {
  level: PriorityLabel;
  rate?: number;
}

const LEVEL_CLASS: Record<PriorityLabel, string> = {
  High: styles.priorityHigh,
  Medium: styles.priorityMedium,
  Low: styles.priorityLow,
};

/**
 * A refactoring's priority by brand intensity and a word: solid, outlined, muted. Never red, amber
 * or green, which mean dependency status.
 *
 * @example <PriorityBadge level="High" rate={0.625} />
 */
export function PriorityBadge({ level, rate }: PriorityBadgeProps) {
  return (
    <span className={`${styles.priority} ${LEVEL_CLASS[level]}`}>
      {level}
      {rate !== undefined && <span className={styles.priorityRate}>{formatRate(rate)}</span>}
    </span>
  );
}

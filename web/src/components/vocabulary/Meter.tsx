import { formatPercent, formatRate } from '../../utils/formatPercent';
import styles from './vocabulary.module.css';

interface MeterProps {
  /** 0..1; values outside are clamped. */
  value: number;
  label?: string;
  /** "percent" (default) for a similarity, "rate" for a recommendation rate. */
  format?: 'percent' | 'rate';
}

/**
 * A 0..1 bar with its number, used instead of a formula.
 *
 * @example <Meter label="Similarity" value={0.75} />
 */
export function Meter({ value, label, format = 'percent' }: MeterProps) {
  const clamped = Math.max(0, Math.min(1, value));
  return (
    <div
      className={styles.meter}
      role="meter"
      aria-valuemin={0}
      aria-valuemax={1}
      aria-valuenow={clamped}
      aria-label={label}
    >
      <div className={styles.meterHead}>
        {label && <span className={styles.meterLabel}>{label}</span>}
        <span className={styles.meterValue}>
          {format === 'rate' ? formatRate(clamped) : formatPercent(clamped)}
        </span>
      </div>
      <div className={styles.meterTrack}>
        <div className={styles.meterFill} style={{ width: `${clamped * 100}%` }} />
      </div>
    </div>
  );
}

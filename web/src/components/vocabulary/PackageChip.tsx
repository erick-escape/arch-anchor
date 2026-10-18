import { PACKAGE_STATUS_WORDS, type PackageStatus } from './dependencyStatus';
import styles from './vocabulary.module.css';

interface PackageChipProps {
  name: string;
  /** allowed: solid accent; divergence: dashed red; absence: dotted amber; none: neutral. */
  status?: PackageStatus;
}

const STATUS_CLASS: Record<PackageStatus, string> = {
  allowed: styles.chipAllowed,
  divergence: styles.chipDivergence,
  absence: styles.chipAbsence,
};

/**
 * An external package, always in mono. Every package name in the UI is this chip.
 *
 * @example <PackageChip name="java.sql" status="divergence" />
 */
export function PackageChip({ name, status }: PackageChipProps) {
  return (
    <span className={[styles.chip, status && STATUS_CLASS[status]].filter(Boolean).join(' ')}>
      {name}
      {status && <span className={styles.visuallyHidden}>, {PACKAGE_STATUS_WORDS[status]}</span>}
    </span>
  );
}

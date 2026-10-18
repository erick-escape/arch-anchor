import styles from './vocabulary.module.css';

export type DependencyKind = 'allow' | 'divergence' | 'absence';

const KINDS: Record<DependencyKind, { className: string; dash?: string; label: string }> = {
  allow: { className: styles.arrowAllow, label: 'allowed dependency' },
  divergence: { className: styles.arrowDivergence, dash: '7 5', label: 'forbidden dependency' },
  absence: {
    className: styles.arrowAbsence,
    dash: '1.5 4.5',
    label: 'missing mandatory dependency',
  },
};

interface DependencyArrowProps {
  kind?: DependencyKind;
  /** Length in px, tail to tip. */
  length?: number;
  /** What the arrow says, when its kind alone does not. */
  label?: string;
}

/**
 * A dependency drawn left to right: solid, dashed or dotted, always with a filled head.
 *
 * @example <DependencyArrow kind="divergence" length={64} />
 */
export function DependencyArrow({ kind = 'allow', length = 120, label }: DependencyArrowProps) {
  const spec = KINDS[kind];
  return (
    <svg
      className={`${styles.arrow} ${spec.className}`}
      width={length}
      height={14}
      viewBox={`0 0 ${length} 14`}
      role="img"
      aria-label={label ?? spec.label}
    >
      <line
        x1={1}
        y1={7}
        x2={length - 11}
        y2={7}
        strokeWidth={2}
        strokeDasharray={spec.dash}
        strokeLinecap="round"
      />
      <polygon points={`${length - 12},1 ${length - 1},7 ${length - 12},13`} />
    </svg>
  );
}

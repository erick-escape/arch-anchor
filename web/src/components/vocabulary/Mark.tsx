import styles from './vocabulary.module.css';

// The mark's geometry, from docs/design/brand/archanchor-mark.svg (measured from the lockup on
// p.18 of 2026_talk_unibz_terra.pdf). The carved shapes are solved as polygons, so the mark needs
// no masks or ids and can appear any number of times on a page.
const CHEVRON = '0,71 50,0 100,71 84.49,71 50,21 15.51,71';
const MOUNTAIN =
  '15.5,87.3 33,63 40.22,73.03 50.5,42 61.0,73.71 68.5,63.5 86,87.3 66.83,87.3 50,72 33.17,87.3';
const BASE = '50,84 65,92 50,100 35,92';
const ARMS = [
  { x: 5.97, y: 51.85, width: 12, height: 25, transform: 'rotate(34.6 11.97 64.35)' },
  { x: 82.03, y: 51.85, width: 12, height: 25, transform: 'rotate(-34.6 88.03 64.35)' },
  { x: 19.78, y: 35.75, width: 8, height: 4.5, transform: 'rotate(34.6 23.78 38)' },
  { x: 72.22, y: 35.75, width: 8, height: 4.5, transform: 'rotate(-34.6 76.22 38)' },
];

interface MarkProps {
  /** Width in px; the height keeps the mark's 104:110 box. */
  size?: number;
  /** "mono" draws the chevron alone in currentColor, for 16-32 px and the reference-class tile. */
  variant?: 'full' | 'mono';
  /** Names the mark for screen readers; without it the mark is decorative. */
  title?: string;
}

/**
 * The ArchAnchor mark, coloured by the mark tokens so it follows the theme.
 *
 * @example <Mark size={28} title="ArchAnchor" />
 */
export function Mark({ size = 40, variant = 'full', title }: MarkProps) {
  const chevron = variant === 'mono' ? styles.markMono : styles.markChevron;
  return (
    <svg
      className={styles.mark}
      viewBox="-2 -4 104 110"
      width={size}
      height={Math.round((size * 110) / 104)}
      role={title ? 'img' : undefined}
      aria-label={title}
      aria-hidden={title ? undefined : true}
    >
      <polygon className={chevron} points={CHEVRON} />
      {ARMS.map((arm) => (
        <rect key={arm.transform} className={chevron} {...arm} />
      ))}
      {variant === 'full' && <polygon className={styles.markMountain} points={MOUNTAIN} />}
      {variant === 'full' && <polygon className={styles.markBase} points={BASE} />}
    </svg>
  );
}

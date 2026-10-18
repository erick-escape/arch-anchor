import { Mark } from './Mark';
import styles from './vocabulary.module.css';

interface LogoProps {
  /** "lockup" (default) is the mark and the two-tone wordmark; "mark" and "mono" the mark alone. */
  variant?: 'lockup' | 'mark' | 'mono';
  /** The mark's width in px. The wordmark is 0.6 × it, as in deck-kit's LOCKUP. */
  size?: number;
  tagline?: boolean;
}

/**
 * The ArchAnchor logo. Once per page, top left of the header, linking to the homepage.
 *
 * @example <Logo size={34} />
 */
export function Logo({ variant = 'lockup', size = 40, tagline = false }: LogoProps) {
  if (variant !== 'lockup') {
    return <Mark size={size} variant={variant === 'mono' ? 'mono' : 'full'} title="ArchAnchor" />;
  }
  return (
    <span className={styles.lockup} style={{ gap: size * 0.24 }} role="img" aria-label="ArchAnchor">
      <Mark size={size} />
      <span className={styles.lockupWords}>
        <span className={styles.wordmark} style={{ fontSize: size * 0.6 }}>
          <span className={styles.wordmarkArch}>Arch</span>
          <span className={styles.wordmarkAnchor}>Anchor</span>
        </span>
        {tagline && (
          <span className={styles.tagline} style={{ fontSize: Math.max(11, size * 0.19) }}>
            Architecture grounded in code.
          </span>
        )}
      </span>
    </span>
  );
}

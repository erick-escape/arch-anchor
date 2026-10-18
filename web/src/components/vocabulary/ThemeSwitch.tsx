import { useTheme } from '../../theme/themeContext';
import { THEME_PREFERENCES, type ThemePreference } from '../../theme/themePreference';
import styles from './vocabulary.module.css';

const LABELS: Record<ThemePreference, string> = { system: 'System', light: 'Light', dark: 'Dark' };

/**
 * System / Light / Dark. System is the default and follows the OS; a choice is remembered per
 * browser.
 *
 * @example <ThemeSwitch />
 */
export function ThemeSwitch() {
  const { preference, setPreference } = useTheme();
  return (
    <div className={styles.segmented} role="group" aria-label="Theme">
      {THEME_PREFERENCES.map((option) => (
        <button
          key={option}
          type="button"
          aria-pressed={preference === option}
          className={
            preference === option ? `${styles.segment} ${styles.segmentOn}` : styles.segment
          }
          onClick={() => setPreference(option)}
        >
          {LABELS[option]}
        </button>
      ))}
    </div>
  );
}

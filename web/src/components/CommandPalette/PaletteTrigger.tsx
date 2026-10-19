import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import styles from './CommandPalette.module.css';
import { useCommandPalette } from './paletteContext';
import { shortcutLabelFor } from './shortcutLabel';

/**
 * The header's way into the palette, showing its shortcut for this platform.
 *
 * @example <AppHeader actions={<PaletteTrigger />} />
 */
export function PaletteTrigger() {
  const palette = useCommandPalette();
  return (
    <button
      type="button"
      aria-label="Open the command palette"
      className={styles.trigger}
      onClick={palette.open}
    >
      <FontAwesomeIcon icon={faMagnifyingGlass} />
      <kbd className={styles.triggerKey}>{shortcutLabelFor(navigator.platform)}</kbd>
    </button>
  );
}

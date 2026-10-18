import type { EnforceMode } from '../../interface/ClazzData';
import styles from './vocabulary.module.css';

const MODES: EnforceMode[] = ['ALLOW', 'MUST'];

// Shown under the control, where today's tooltip only appeared on hover.
const EXPLANATIONS: Record<EnforceMode, string> = {
  ALLOW: 'Peer classes may use any subset of these dependencies. Exported as CAN-DEPEND.',
  MUST: 'Peer classes must include all of these dependencies. Exported as MUST-DEPEND.',
};

interface ModeToggleProps {
  value: EnforceMode;
  onChange: (mode: EnforceMode) => void;
  /** Names the radio group, e.g. "CatalogController enforce mode". */
  label: string;
  disabled?: boolean;
}

/**
 * A reference class's enforce mode as a two-option segmented control, with the mode in force
 * explained in words.
 *
 * @example <ModeToggle value="ALLOW" onChange={setMode} label="CatalogController enforce mode" />
 */
export function ModeToggle({ value, onChange, label, disabled = false }: ModeToggleProps) {
  return (
    <div className={styles.mode}>
      <div className={styles.modeGroup} role="radiogroup" aria-label={label}>
        {MODES.map((mode) => (
          <button
            key={mode}
            type="button"
            role="radio"
            aria-checked={value === mode}
            disabled={disabled}
            className={value === mode ? `${styles.modeOption} ${styles.modeOn}` : styles.modeOption}
            onClick={() => mode !== value && onChange(mode)}
          >
            {mode}
          </button>
        ))}
      </div>
      <span className={styles.modeText}>{EXPLANATIONS[value]}</span>
    </div>
  );
}

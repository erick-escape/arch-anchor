import styles from './Sidebar.module.css';

interface SectionHeaderProps {
  title: string;
  expanded: boolean;
  onToggle: () => void;
}

/**
 * The title of a collapsible sidebar section. It is a button, so it can be reached and toggled
 * from the keyboard, and it shows whether the section is open.
 *
 * @example
 * <SectionHeader title="Dependencies" expanded={open} onToggle={() => setOpen(!open)} />
 */
const SectionHeader = ({ title, expanded, onToggle }: SectionHeaderProps) => (
  <button
    type="button"
    className={styles.sectionHeader}
    style={{ marginBottom: expanded ? '10px' : '0' }}
    aria-expanded={expanded}
    onClick={onToggle}
  >
    <span>{title}</span>
    {/* Hidden from assistive technology: aria-expanded already says it. */}
    <span aria-hidden="true">{expanded ? '−' : '+'}</span>
  </button>
);

export default SectionHeader;

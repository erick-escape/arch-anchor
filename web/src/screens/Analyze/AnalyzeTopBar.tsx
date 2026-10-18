import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faDownload, faSliders } from '@fortawesome/free-solid-svg-icons';
import { Button } from 'antd';
import { Link } from 'react-router-dom';
import { Logo } from '../../components/vocabulary/Logo';
import { ThemeSwitch } from '../../components/vocabulary/ThemeSwitch';
import styles from './Analyze.module.css';

interface AnalyzeTopBarProps {
  projectName: string;
  /** False while there is nothing to export: during the analysis, or with no modules. */
  canExport: boolean;
  onExport: () => void;
  onOpenSettings: () => void;
}

/**
 * The Analyze page's compact bar: the mark home, the way back to the projects, and the page's
 * actions in the open, where they used to sit behind a ⋮ menu.
 *
 * @example <AnalyzeTopBar projectName="bookstore" canExport onExport={exportAcs} onOpenSettings={open} />
 */
export function AnalyzeTopBar({
  projectName,
  canExport,
  onExport,
  onOpenSettings,
}: AnalyzeTopBarProps) {
  return (
    <header className={styles.topBar}>
      <Link to="/" aria-label="ArchAnchor home" className={styles.home}>
        <Logo variant="mark" size={26} />
      </Link>
      <nav aria-label="Breadcrumb" className={styles.breadcrumb}>
        <Link to="/projects">Projects</Link>
        <span aria-hidden>/</span>
        <span aria-current="page" className={styles.current}>
          {projectName}
        </span>
      </nav>
      <div className={styles.spacer} />
      <Button icon={<FontAwesomeIcon icon={faDownload} />} disabled={!canExport} onClick={onExport}>
        Export ACs
      </Button>
      <Button icon={<FontAwesomeIcon icon={faSliders} />} onClick={onOpenSettings}>
        Settings
      </Button>
      <ThemeSwitch />
    </header>
  );
}

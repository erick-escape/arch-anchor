import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faDownload,
  faLayerGroup,
  faListCheck,
  faSliders,
} from '@fortawesome/free-solid-svg-icons';
import { Button } from 'antd';
import { Link } from 'react-router-dom';
import { PaletteTrigger } from '../../components/CommandPalette/PaletteTrigger';
import { Logo } from '../../components/vocabulary/Logo';
import { ThemeSwitch } from '../../components/vocabulary/ThemeSwitch';
import type { PageActions } from './analyzeCommands';
import styles from './Analyze.module.css';

interface AnalyzeTopBarProps {
  projectName: string;
  /** False while there is nothing to export: during the analysis, or with no modules. */
  canExport: boolean;
  actions: PageActions;
}

/**
 * The Analyze page's compact bar: the mark home, the way back to the projects, and the page's
 * actions in the open, where they used to sit behind a ⋮ menu.
 *
 * @example <AnalyzeTopBar projectName="bookstore" canExport actions={actions} />
 */
export function AnalyzeTopBar({ projectName, canExport, actions }: AnalyzeTopBarProps) {
  return (
    <header className={styles.topBar}>
      <Link to="/" aria-label="ArchAnchor home" className={styles.home}>
        <Logo variant="mark" size={26} />
      </Link>
      <nav aria-label="Breadcrumb" className={styles.breadcrumb}>
        <Link to="/projects" className={styles.parentCrumb}>
          Projects
        </Link>
        <span aria-hidden className={styles.parentCrumb}>
          /
        </span>
        <span aria-current="page" className={styles.current}>
          {projectName}
        </span>
      </nav>
      <div className={styles.spacer} />
      <Button
        icon={<FontAwesomeIcon icon={faLayerGroup} />}
        aria-expanded={actions.open.modules}
        aria-controls="modules-panel"
        onClick={() => actions.toggle('modules')}
      >
        <span className={styles.barLabel}>Modules</span>
      </Button>
      <Button
        icon={<FontAwesomeIcon icon={faListCheck} />}
        aria-expanded={actions.open.recommendations}
        aria-controls="recommendations-panel"
        onClick={() => actions.toggle('recommendations')}
      >
        <span className={styles.barLabel}>Recommendations</span>
      </Button>
      <Button
        icon={<FontAwesomeIcon icon={faDownload} />}
        disabled={!canExport}
        onClick={actions.exportAcs}
      >
        <span className={styles.barLabel}>Export ACs</span>
      </Button>
      <Button icon={<FontAwesomeIcon icon={faSliders} />} onClick={actions.openSettings}>
        <span className={styles.barLabel}>Settings</span>
      </Button>
      <span className={styles.themeSlot}>
        <ThemeSwitch />
      </span>
      <PaletteTrigger />
    </header>
  );
}

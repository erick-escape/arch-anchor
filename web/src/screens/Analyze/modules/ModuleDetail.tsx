import { faChevronLeft } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { ClassTile } from '../../../components/vocabulary/ClassTile';
import { ModeToggle } from '../../../components/vocabulary/ModeToggle';
import type { ModuleData } from '../../../interface/ModuleData';
import { formatPercent } from '../../../utils/formatPercent';
import { violationsOf } from '../graph/moduleContract';
import type { ModuleOperations } from '../useModuleOperations';
import { ClassSelection } from './ClassSelection';
import { ModuleMenu, type ModuleMenuActions } from './ModuleMenu';
import styles from './ModulesPanel.module.css';

interface ModuleDetailProps {
  module: ModuleData;
  operations: ModuleOperations;
  actions: ModuleMenuActions;
  onBack: () => void;
}

/**
 * One module: its similarity and violations, its reference classes with their enforce mode, its
 * classes (to select for a split or as new reference classes) and its dependencies as chips.
 */
export function ModuleDetail({ module, operations, actions, onBack }: ModuleDetailProps) {
  return (
    <>
      <div className={styles.detailHead}>
        <button
          type="button"
          className={styles.iconButton}
          aria-label="Back to all modules"
          onClick={onBack}
        >
          <FontAwesomeIcon icon={faChevronLeft} />
        </button>
        <h2 className={styles.panelTitle}>{module.name}</h2>
        <ModuleMenu module={module} actions={actions} />
      </div>
      <ClassSelection module={module} operations={operations}>
        <ModuleStats module={module} />
        <ReferenceClasses module={module} operations={operations} />
      </ClassSelection>
    </>
  );
}

function ModuleStats({ module }: { module: ModuleData }) {
  const violations = violationsOf(module);
  return (
    <div className={styles.stats}>
      <div className={styles.stat}>
        <span className={styles.statValue}>{formatPercent(module.similarity)}</span>
        <span>module similarity</span>
      </div>
      <div className={styles.stat}>
        <span className={violations.length ? styles.statViolations : styles.statValue}>
          {violations.length}
        </span>
        <span>{violations.length === 1 ? `violation: ${violations[0]}` : 'violations'}</span>
      </div>
    </div>
  );
}

function ReferenceClasses({
  module,
  operations,
}: {
  module: ModuleData;
  operations: ModuleOperations;
}) {
  const title = module.refClazzes.length === 1 ? 'Reference class' : 'Reference classes';
  return (
    <section aria-label={title} className={styles.section}>
      <h3 className={styles.overline}>{title}</h3>
      {module.refClazzes.length === 0 && <span className={styles.muted}>None elected.</span>}
      {module.refClazzes.map((reference) => (
        <div key={reference.id} className={styles.reference}>
          <div className={styles.referenceHead}>
            <ClassTile reference size={30} />
            <span className={styles.referenceName}>{reference.name}</span>
            <span className={styles.mono}>{formatPercent(reference.similarity)}</span>
          </div>
          <ModeToggle
            value={reference.enforceMode ?? 'ALLOW'}
            label={`${reference.name} enforce mode`}
            onChange={(mode) => void operations.setMode(module, reference.id, mode)}
          />
        </div>
      ))}
    </section>
  );
}

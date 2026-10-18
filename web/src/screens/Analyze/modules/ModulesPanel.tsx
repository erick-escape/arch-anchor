import { useState } from 'react';
import type { ModuleData } from '../../../interface/ModuleData';
import type { ModuleOperations } from '../useModuleOperations';
import { DeleteModuleDialog } from './ModuleDialogs';
import { ModuleDetail } from './ModuleDetail';
import { ModuleList } from './ModuleList';
import type { ModuleMenuActions } from './ModuleMenu';
import styles from './ModulesPanel.module.css';

interface ModulesPanelProps {
  modules: ModuleData[];
  operations: ModuleOperations;
  /** Asks the page to confirm a merge, the way dropping one module onto another does. */
  onProposeMerge: (source: ModuleData) => void;
}

/**
 * The modules panel, floating over the graph's left edge: the list of modules, or one module's
 * reference classes, classes and dependencies. It keeps only the id of the module it shows and
 * reads the module from the page, so it always shows the current one.
 *
 * @example <ModulesPanel modules={modules} operations={operations} onProposeMerge={propose} />
 */
export function ModulesPanel({ modules, operations, onProposeMerge }: ModulesPanelProps) {
  const [shownId, setShownId] = useState<string | null>(null);
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<ModuleData | null>(null);
  const shown = modules.find((module) => module.id === shownId);
  const actions: ModuleMenuActions = {
    onRename: (module) => {
      setShownId(null);
      setRenamingId(module.id);
    },
    onMerge: onProposeMerge,
    onDelete: setToDelete,
  };
  return (
    <aside aria-label="Modules" className={styles.panel}>
      {shown ? (
        <ModuleDetail
          key={shown.id}
          module={shown}
          operations={operations}
          actions={actions}
          onBack={() => setShownId(null)}
        />
      ) : (
        <ModuleList
          modules={modules}
          renamingId={renamingId}
          onRenamed={() => setRenamingId(null)}
          onOpen={setShownId}
          operations={operations}
          actions={actions}
        />
      )}
      <DeleteModuleDialog
        module={toDelete}
        operations={operations}
        onClose={() => setToDelete(null)}
      />
    </aside>
  );
}

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
  /** The module whose detail is open, or null for the list; held by the page for the palette. */
  shownId: string | null;
  onShow: (moduleId: string | null) => void;
  hidden?: boolean;
}

/**
 * The modules panel, floating over the graph's left edge: the list of modules, or one module's
 * reference classes, classes and dependencies. The page holds only the id of the module shown,
 * and the panel reads the module from the page, so it always shows the current one.
 *
 * @example <ModulesPanel modules={modules} operations={operations} onProposeMerge={propose} shownId={id} onShow={setId} />
 */
export function ModulesPanel(props: ModulesPanelProps) {
  const { modules, operations, onProposeMerge, shownId, onShow: setShownId, hidden } = props;
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
    <aside id="modules-panel" aria-label="Modules" className={styles.panel} hidden={hidden}>
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

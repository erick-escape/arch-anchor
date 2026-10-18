import { Input } from 'antd';
import { useState, type KeyboardEvent } from 'react';
import { Meter } from '../../../components/vocabulary/Meter';
import type { ModuleData } from '../../../interface/ModuleData';
import { violationsOf } from '../graph/moduleContract';
import type { ModuleOperations } from '../useModuleOperations';
import { ModuleMenu, type ModuleMenuActions } from './ModuleMenu';
import styles from './ModulesPanel.module.css';

interface ModuleListProps {
  modules: ModuleData[];
  renamingId: string | null;
  onRenamed: () => void;
  onOpen: (moduleId: string) => void;
  operations: ModuleOperations;
  actions: ModuleMenuActions;
}

/** Every module, searchable by name, each with its reference class, similarity and violations. */
export function ModuleList(props: ModuleListProps) {
  const [query, setQuery] = useState('');
  const shown = props.modules.filter((module) =>
    module.name.toLowerCase().includes(query.trim().toLowerCase())
  );
  return (
    <>
      <div className={styles.listHead}>
        <div className={styles.titleRow}>
          <h2 className={styles.panelTitle}>Modules</h2>
          <span className={styles.count}>{props.modules.length} modules</span>
        </div>
        <Input
          type="search"
          aria-label="Search modules"
          placeholder="Search modules"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          allowClear
        />
      </div>
      <ul className={styles.list}>
        {shown.map((module) => (
          <li key={module.id} className={styles.row}>
            {props.renamingId === module.id ? (
              <RenameField module={module} operations={props.operations} onDone={props.onRenamed} />
            ) : (
              <ModuleRowButton module={module} onOpen={() => props.onOpen(module.id)} />
            )}
            <ModuleMenu module={module} actions={props.actions} />
          </li>
        ))}
      </ul>
    </>
  );
}

function ModuleRowButton({ module, onOpen }: { module: ModuleData; onOpen: () => void }) {
  const violations = violationsOf(module).length;
  const references = module.refClazzes.map((reference) => reference.name).join(' · ');
  return (
    <button type="button" className={styles.rowButton} onClick={onOpen}>
      <span className={styles.rowTop}>
        <span className={styles.rowName}>{module.name}</span>
        {violations > 0 && (
          <span className={styles.rowViolations}>
            {violations === 1 ? '1 violation' : `${violations} violations`}
          </span>
        )}
      </span>
      <Meter label={references || 'no reference class'} value={module.similarity} />
    </button>
  );
}

interface RenameFieldProps {
  module: ModuleData;
  operations: ModuleOperations;
  onDone: () => void;
}

// Enter saves and Escape cancels; a blank or unchanged name changes nothing.
function RenameField({ module, operations, onDone }: RenameFieldProps) {
  const [name, setName] = useState(module.name);
  const save = async () => {
    const trimmed = name.trim();
    if (!trimmed || trimmed === module.name) return onDone();
    if (await operations.rename(module, trimmed)) onDone();
  };
  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') void save();
    if (event.key === 'Escape') onDone();
  };
  return (
    <div className={styles.rename}>
      <Input
        autoFocus
        aria-label={`New name for ${module.name}`}
        value={name}
        onChange={(event) => setName(event.target.value)}
        onKeyDown={onKeyDown}
      />
      <span className={styles.renameHint}>Enter saves · Esc cancels</span>
    </div>
  );
}

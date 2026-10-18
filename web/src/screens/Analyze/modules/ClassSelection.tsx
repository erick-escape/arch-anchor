import { Button } from 'antd';
import { useState, type ReactNode } from 'react';
import { PackageChip } from '../../../components/vocabulary/PackageChip';
import type { ModuleData } from '../../../interface/ModuleData';
import { formatPercent } from '../../../utils/formatPercent';
import { classStatusOf, contractOf } from '../graph/moduleContract';
import type { ModuleOperations } from '../useModuleOperations';
import styles from './ModulesPanel.module.css';

interface ClassSelectionProps {
  module: ModuleData;
  operations: ModuleOperations;
  /** What sits above the classes in the scrolling body: stats, reference classes. */
  children: ReactNode;
}

/**
 * The module's classes, each with a checkbox, and the toolbar that appears once any is selected:
 * Set as reference, Split, Clear. These used to sit behind a right-click nobody would guess.
 */
export function ClassSelection({ module, operations, children }: ClassSelectionProps) {
  const [selected, toggle, clear] = useClassSelection();
  const run = async (change: Promise<boolean>) => {
    if (await change) clear();
  };
  return (
    <>
      <div className={styles.body}>
        {children}
        <section aria-label="Classes" className={styles.section}>
          <h3 className={styles.overline}>Classes ({module.clazzes.length})</h3>
          {module.clazzes.map((clazz) => (
            <ClassRow
              key={clazz.id}
              module={module}
              classId={clazz.id}
              checked={selected.includes(clazz.id)}
              onToggle={toggle}
            />
          ))}
        </section>
        <ModuleDependencies module={module} />
      </div>
      {selected.length > 0 && (
        <div role="toolbar" aria-label="Selected classes" className={styles.toolbar}>
          <span className={styles.toolbarCount}>{selected.length} selected</span>
          <Button size="small" onClick={() => run(operations.setReferences(module, selected))}>
            Set as reference
          </Button>
          <Button size="small" onClick={() => run(operations.split(module, selected))}>
            Split
          </Button>
          <Button size="small" type="link" onClick={clear}>
            Clear
          </Button>
        </div>
      )}
    </>
  );
}

interface ClassRowProps {
  module: ModuleData;
  classId: string;
  checked: boolean;
  onToggle: (classId: string) => void;
}

function ClassRow({ module, classId, checked, onToggle }: ClassRowProps) {
  const clazz = module.clazzes.find((candidate) => candidate.id === classId)!;
  const reference = module.refClazzes.some((candidate) => candidate.id === classId);
  const status = classStatusOf(clazz, module);
  const nameClass = reference
    ? styles.classReference
    : status === 'divergence'
      ? styles.classDivergent
      : styles.className;
  return (
    <label className={checked ? `${styles.classRow} ${styles.classRowSelected}` : styles.classRow}>
      <input
        type="checkbox"
        checked={checked}
        onChange={() => onToggle(classId)}
        aria-label={`Select ${clazz.name}`}
      />
      <span className={styles.classBody}>
        <span className={styles.classTop}>
          <span className={nameClass}>{clazz.name}</span>
          <span className={styles.mono}>{formatPercent(clazz.similarity)}</span>
        </span>
        <span className={styles.classBar} aria-hidden>
          <span
            className={status === 'divergence' ? styles.classFillDivergent : styles.classFill}
            style={{ width: `${clazz.similarity * 100}%` }}
          />
        </span>
      </span>
    </label>
  );
}

/** The module's dependencies: solid when its reference classes allow them, dashed when not. */
function ModuleDependencies({ module }: { module: ModuleData }) {
  const contract = contractOf(module);
  return (
    <section aria-label="Dependencies" className={styles.section}>
      <h3 className={styles.overline}>Dependencies</h3>
      {module.allDependencies.length === 0 && <span className={styles.muted}>No dependencies</span>}
      <div className={styles.chips}>
        {module.allDependencies.map(({ packageName }) => (
          <PackageChip
            key={packageName}
            name={packageName}
            status={contract.has(packageName) ? 'allowed' : 'divergence'}
          />
        ))}
      </div>
      <span className={styles.caption}>
        Solid: in the reference classes’ contract. Dashed: a violation.
      </span>
    </section>
  );
}

function useClassSelection(): [string[], (classId: string) => void, () => void] {
  const [selected, setSelected] = useState<string[]>([]);
  const toggle = (classId: string) =>
    setSelected((current) =>
      current.includes(classId) ? current.filter((id) => id !== classId) : [...current, classId]
    );
  return [selected, toggle, () => setSelected([])];
}

import { Handle, Position, type NodeProps } from '@xyflow/react';
import { ClassTile } from '../../../components/vocabulary/ClassTile';
import { ModuleContainer } from '../../../components/vocabulary/ModuleContainer';
import type { ClazzData } from '../../../interface/ClazzData';
import type { ModuleData } from '../../../interface/ModuleData';
import type { ModuleNode } from './graphElements';
import styles from './graph.module.css';
import { classStatusOf, violationsOf } from './moduleContract';

// Two rows of six tiles; a larger module shows the rest as a count.
const SHOWN_TILES = 12;

/**
 * A module on the graph: its classes as tiles, reference classes first in accent, a class that
 * breaks the contract outlined by its status, the reference class names, the similarity meter
 * and the violation count.
 */
export function ModuleNodeView({ data }: NodeProps<ModuleNode>) {
  const { module, highlighted } = data;
  const classes = referenceClassesFirst(module);
  const hidden = classes.length - SHOWN_TILES;
  return (
    <>
      <Handle type="target" position={Position.Top} className={styles.handle} />
      <ModuleContainer
        name={module.name}
        similarity={module.similarity}
        violations={violationsOf(module).length}
        highlighted={highlighted}
      >
        {classes.slice(0, SHOWN_TILES).map((clazz) => (
          <ClassTile
            key={clazz.id}
            size={28}
            reference={isReference(clazz, module)}
            status={classStatusOf(clazz, module)}
          />
        ))}
        {hidden > 0 && <span className={styles.moreClasses}>+{hidden}</span>}
        <span className={styles.referenceNames}>{referenceNamesOf(module)}</span>
      </ModuleContainer>
      <Handle type="source" position={Position.Bottom} className={styles.handle} />
    </>
  );
}

function isReference(clazz: ClazzData, module: ModuleData): boolean {
  return module.refClazzes.some((reference) => reference.id === clazz.id);
}

function referenceClassesFirst(module: ModuleData): ClazzData[] {
  const others = module.clazzes.filter((clazz) => !isReference(clazz, module));
  const references = module.clazzes.filter((clazz) => isReference(clazz, module));
  return [...references, ...others];
}

function referenceNamesOf(module: ModuleData): string {
  if (module.refClazzes.length === 0) return 'no reference class';
  return module.refClazzes.map((reference) => reference.name).join(' · ');
}

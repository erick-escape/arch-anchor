import type { ReactNode } from 'react';
import { Meter } from './Meter';
import styles from './vocabulary.module.css';

interface ModuleContainerProps {
  name: string;
  /** 0..1, drawn as a meter. */
  similarity?: number;
  violations?: number;
  /** The glow for a module the hovered recommendation touches. */
  highlighted?: boolean;
  className?: string;
  /** The class tiles, reference class first. */
  children?: ReactNode;
}

/**
 * A module: a sunken container with its name on a tab riding the top edge. The graph node of the
 * Analyze page and the illustration everywhere else.
 *
 * @example <ModuleContainer name="bookstore/web" similarity={0.75} violations={1}>{tiles}</ModuleContainer>
 */
export function ModuleContainer(props: ModuleContainerProps) {
  const { name, similarity, violations, highlighted = false, className, children } = props;
  const classes = [styles.module, highlighted && styles.moduleHighlighted, className];
  return (
    <section className={classes.filter(Boolean).join(' ')} aria-label={`Module ${name}`}>
      <span className={styles.moduleTab}>{name}</span>
      <div className={styles.moduleBody}>{children}</div>
      {(similarity !== undefined || Boolean(violations)) && (
        <div className={styles.moduleFoot}>
          {similarity !== undefined && <Meter value={similarity} label="Similarity" />}
          {Boolean(violations) && <ViolationCount count={violations ?? 0} />}
        </div>
      )}
    </section>
  );
}

function ViolationCount({ count }: { count: number }) {
  return (
    <span className={styles.moduleViolations}>
      {count} {count === 1 ? 'violation' : 'violations'}
    </span>
  );
}

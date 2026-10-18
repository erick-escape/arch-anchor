import { useReactFlow } from '@xyflow/react';
import { Switch } from 'antd';
import type { ModuleData } from '../../interface/ModuleData';
import { architecturalConstraintsOf } from '../../utils/exportACs';
import { formatPercent } from '../../utils/formatPercent';
import styles from './Analyze.module.css';
import { projectSimilarityOf, totalViolationsOf } from './graph/moduleContract';

function plural(count: number, word: string, words = `${word}s`): string {
  return `${count} ${count === 1 ? word : words}`;
}

/** The constraints Export ACs would write: one per dependency of each reference class. */
function constraintCountOf(modules: ModuleData[]): number {
  return architecturalConstraintsOf(modules)
    .flatMap((module) => module.refClassConstraints)
    .reduce((count, constraint) => count + constraint.dependencies.length, 0);
}

/**
 * The page's heading states what the graph shows, "bookstore · 4 modules · 1 violation", where it
 * used to say only "Analyzed Modules for Project:". Below it, the project's headline numbers.
 */
export function GraphHeading({
  projectName,
  modules,
}: {
  projectName: string;
  modules: ModuleData[];
}) {
  const violations = totalViolationsOf(modules);
  const classes = modules.reduce((count, module) => count + module.clazzes.length, 0);
  return (
    <div className={styles.heading}>
      <h1 className={styles.title}>
        {projectName} · {plural(modules.length, 'module')} ·{' '}
        {violations === 0 ? 'no violations' : plural(violations, 'violation')}
      </h1>
      <div className={styles.stats}>
        <span>
          Project similarity <b>{formatPercent(projectSimilarityOf(modules))}</b>
        </span>
        <span>{plural(classes, 'class', 'classes')}</span>
        <span>{plural(constraintCountOf(modules), 'constraint')}</span>
      </div>
    </div>
  );
}

interface GraphLegendProps {
  showExternal: boolean;
  onShowExternalChange: (show: boolean) => void;
}

/** What the drawings mean, and whether packages outside the project are drawn. */
export function GraphLegend({ showExternal, onShowExternalChange }: GraphLegendProps) {
  return (
    <div className={styles.legend} aria-label="Legend" role="group">
      <span className={styles.legendItem}>
        <span className={styles.legendReference} aria-hidden /> reference class
      </span>
      <span className={styles.legendItem}>
        <span className={styles.legendAllowed} aria-hidden /> allowed
      </span>
      <span className={styles.legendItem}>
        <span className={styles.legendDivergence} aria-hidden /> violation
      </span>
      <span className={styles.legendItem}>
        <span className={styles.legendAbsence} aria-hidden /> missing MUST
      </span>
      <label className={styles.legendItem}>
        <Switch size="small" checked={showExternal} onChange={onShowExternalChange} />
        external packages
      </label>
    </div>
  );
}

/** Zoom in, zoom out and fit, as labelled buttons. */
export function ZoomControls() {
  const { zoomIn, zoomOut, fitView } = useReactFlow();
  return (
    <div className={styles.zoom} role="group" aria-label="Zoom">
      <button type="button" aria-label="Zoom in" onClick={() => zoomIn()}>
        +
      </button>
      <button type="button" aria-label="Zoom out" onClick={() => zoomOut()}>
        −
      </button>
      <button
        type="button"
        aria-label="Fit the graph to the screen"
        onClick={() => fitView({ padding: 0.2 })}
      >
        ⤢
      </button>
    </div>
  );
}

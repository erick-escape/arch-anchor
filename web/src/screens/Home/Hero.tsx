import { ImportBar } from './importBar/ImportBar';
import type { ImportFields } from './importBar/useImportFields';
import type { ProjectImport } from './importBar/useProjectImport';
import styles from './Home.module.css';

// Class tiles and dependencies drifting behind the hero: the vocabulary, out of focus.
const BACKDROP = [
  'tileA',
  'tileB',
  'tileC',
  'edgeA',
  'tileD',
  'tileE',
  'tileF',
  'edgeB',
  'tileG',
  'tileH',
] as const;

interface HeroProps {
  fields: ImportFields;
  projectImport: ProjectImport;
}

/**
 * The top of the homepage: what the tool finds, and the import bar to start from.
 *
 * @example <Hero fields={fields} projectImport={projectImport} />
 */
export function Hero({ fields, projectImport }: HeroProps) {
  return (
    <section aria-labelledby="hero-title" className={styles.hero}>
      <div aria-hidden className={styles.backdrop}>
        <div className={styles.drift}>
          {BACKDROP.map((shape) => (
            <span key={shape} className={styles[shape]} />
          ))}
        </div>
      </div>
      <div className={styles.heroInner}>
        <h1 id="hero-title" className={styles.heroTitle}>
          Find the architecture already in your Java code.
        </h1>
        <p className={styles.lead}>
          Paste a GitHub repository or drop a project folder. ArchAnchor elects a reference class in
          every module and turns its dependencies into the rules the module follows.
        </p>
        <ImportBar fields={fields} projectImport={projectImport} />
        <div className={styles.tryRow}>
          <button type="button" className={styles.tryChip} onClick={fields.fillBookstore}>
            <span aria-hidden className={styles.trySwatch} />
            Try it on the bookstore example
          </button>
          <span className={styles.tryHint}>or drop a project folder anywhere on this page</span>
        </div>
      </div>
    </section>
  );
}

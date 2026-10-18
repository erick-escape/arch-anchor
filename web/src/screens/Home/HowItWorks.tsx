import { faFolder } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import type { ReactNode } from 'react';
import { ClassTile } from '../../components/vocabulary/ClassTile';
import { DependencyArrow } from '../../components/vocabulary/DependencyArrow';
import { PackageChip } from '../../components/vocabulary/PackageChip';
import styles from './Home.module.css';

const STEPS: Array<{ title: string; text: string; picture: ReactNode }> = [
  {
    title: 'Import',
    text: 'Clone a GitHub repository, public or private, at any branch or tag, or upload a folder. Modules come from the package structure.',
    picture: (
      <>
        <span className={styles.repoPill}>owner/repo</span>
        <span className={styles.or}>or</span>
        <FontAwesomeIcon icon={faFolder} className={styles.folder} />
      </>
    ),
  },
  {
    title: 'Elect the anchors',
    text: 'In each module, the class whose dependencies best represent its peers, with the fewest violations, becomes the reference class. You can pick it yourself.',
    picture: (
      <>
        <ClassTile size={36} />
        <ClassTile reference size={44} />
        <ClassTile size={36} />
        <ClassTile size={36} />
      </>
    ),
  },
  {
    title: 'Act on violations and refactorings',
    text: "See every dependency outside a module's contract, and the split, merge or move that would remove it. Export the constraints as a PDF.",
    picture: (
      <>
        <ClassTile size={36} status="divergence" />
        <DependencyArrow kind="divergence" length={64} />
        <PackageChip name="java.sql" status="divergence" />
      </>
    ),
  },
];

/**
 * The three steps from a repository to its constraints, the target of the header's
 * "How it works" (/#how).
 *
 * @example <HowItWorks />
 */
export function HowItWorks() {
  return (
    <section id="how" aria-labelledby="how-title" className={styles.how}>
      <h2 id="how-title" className={styles.sectionTitle}>
        How it works
      </h2>
      <ol className={styles.steps}>
        {STEPS.map((step, index) => (
          <li key={step.title} className={styles.stepCard}>
            <span className={styles.stepNumber}>{index + 1}</span>
            <div aria-hidden className={styles.stepPicture}>
              {step.picture}
            </div>
            <h3 className={styles.stepTitle}>{step.title}</h3>
            <p className={styles.stepText}>{step.text}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}

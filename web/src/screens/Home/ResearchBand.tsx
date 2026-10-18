import { REPOSITORY_URL } from '../../components/AppHeader/AppHeader';
import { Logo } from '../../components/vocabulary/Logo';
import styles from './Home.module.css';

const ZENODO_URL = 'https://zenodo.org/records/20057336';

/**
 * The paper behind the tool and its replication package, the target of the header's "Research"
 * (/#research). Same citation and result as the README's "Citation and research artifact".
 *
 * @example <ResearchBand />
 */
export function ResearchBand() {
  return (
    <section id="research" aria-labelledby="research-title" className={styles.research}>
      <div className={styles.researchInner}>
        <div className={styles.citation}>
          <h2 id="research-title" className={styles.eyebrow}>
            Research
          </h2>
          <p className={styles.paper}>
            <b>
              Towards the Extraction of Architectural Constraints centered on the Reference Class
              Concept.
            </b>{' '}
            Érick de Castro Silva, Eduardo Guerra, Ricardo Terra. ECSA 2026.
          </p>
          <p className={styles.result}>
            Applied to LEONA, a real-world Java system, the tool recovered 21 of 39 manually defined
            constraints and surfaced 20 more that had never been documented.
          </p>
          <div className={styles.researchLinks}>
            <a href={ZENODO_URL} className={styles.textLink}>
              Replication package on Zenodo
            </a>
            <a href={REPOSITORY_URL} className={styles.textLink}>
              Source on GitHub
            </a>
          </div>
        </div>
        <div className={styles.signature}>
          <Logo size={30} tagline />
          <span className={styles.affiliations}>
            Federal University of Lavras · Free University of Bozen-Bolzano · MIT License
          </span>
        </div>
      </div>
    </section>
  );
}

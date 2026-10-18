import { ClassTile } from '../../../components/vocabulary/ClassTile';
import { DependencyArrow } from '../../../components/vocabulary/DependencyArrow';
import { Meter } from '../../../components/vocabulary/Meter';
import { ModuleContainer } from '../../../components/vocabulary/ModuleContainer';
import { PackageChip } from '../../../components/vocabulary/PackageChip';
import styles from './Claims.module.css';

// The vignettes draw the bookstore example as the tool shows it; the numbers are those of
// examples/bookstore/README.md, so a claim never shows what the tool would not.

/** The modules as they are found: classes in their packages, nothing declared. */
export function ModulesFound() {
  return (
    <div className={styles.modules}>
      <ModuleContainer name="bookstore/web">
        <ClassTile name="CatalogController" size={40} />
        <ClassTile name="OrderController" size={40} />
        <ClassTile name="SalesReport" size={40} />
      </ModuleContainer>
      <ModuleContainer name="bookstore/service">
        <ClassTile name="CatalogService" size={40} />
        <ClassTile name="OrderService" size={40} />
      </ModuleContainer>
    </div>
  );
}

const ELECTION = [
  { name: 'CatalogController', similarity: 0.8333, elected: true },
  { name: 'CustomerController', similarity: 0.8333 },
  { name: 'OrderController', similarity: 0.8333 },
  { name: 'SalesReport', similarity: 0.5, divergent: true },
];

/** The election in bookstore/web: the class most like its peers becomes the reference class. */
export function Election() {
  return (
    <div className={styles.election}>
      {ELECTION.map(({ name, similarity, elected, divergent }) => (
        <div key={name} className={styles.candidate}>
          <span className={elected ? styles.elected : undefined}>
            <ClassTile
              size={40}
              reference={elected}
              status={divergent ? 'divergence' : undefined}
            />
          </span>
          <Meter label={name} value={similarity} />
        </div>
      ))}
    </div>
  );
}

/** A repository becomes a reference class per module, with no model written first. */
export function NoModel() {
  return (
    <div className={styles.noModel}>
      <span className={styles.repo}>erick-escape/arch-anchor</span>
      <DependencyArrow length={64} label="becomes" />
      <div className={styles.anchors}>
        {['web', 'service', 'persistence', 'model'].map((module) => (
          <ClassTile key={module} name={module} reference size={36} />
        ))}
      </div>
    </div>
  );
}

/** The reference class's dependencies are the module's rules; SalesReport breaks one. */
export function RulesInCode() {
  return (
    <div className={styles.rules}>
      <ClassTile name="CatalogController" reference size={56} />
      <div className={styles.ruleList}>
        <Rule
          chip={<PackageChip name="bookstore.service" status="allowed" />}
          text="bookstore/web CAN-DEPEND bookstore.service"
        />
        <Rule
          chip={<PackageChip name="bookstore.model" status="allowed" />}
          text="bookstore/web CAN-DEPEND bookstore.model"
        />
        <Rule
          chip={<PackageChip name="java.sql" status="divergence" />}
          text="SalesReport breaks the rule"
          broken
        />
      </div>
    </div>
  );
}

function Rule({ chip, text, broken }: { chip: JSX.Element; text: string; broken?: boolean }) {
  return (
    <div className={styles.rule}>
      {chip}
      <span className={broken ? styles.ruleBroken : styles.ruleText}>{text}</span>
    </div>
  );
}

const CONSTRAINTS = [
  'AC1 - bookstore/model CAN-DEPEND java.time',
  'AC2 - bookstore/persistence CAN-DEPEND bookstore.model',
  'AC3 - bookstore/persistence CAN-DEPEND java.sql',
  'AC4 - bookstore/service CAN-DEPEND bookstore.model',
];

/** The exported constraints: the start of the architecture document nobody wrote. */
export function ConstraintsExport() {
  return (
    <div className={styles.document}>
      <span className={styles.documentTitle}>Architectural constraints for bookstore</span>
      {CONSTRAINTS.map((line) => (
        <span key={line} className={styles.documentLine}>
          {line}
        </span>
      ))}
      <span className={styles.documentMore}>… 3 more</span>
    </div>
  );
}

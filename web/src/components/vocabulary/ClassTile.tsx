import { CLASS_STATUS_WORDS, type DependencyStatus } from './dependencyStatus';
import { Mark } from './Mark';
import styles from './vocabulary.module.css';

interface ClassTileProps {
  name?: string;
  /** The reference class: the same square in accent, wearing the chevron. */
  reference?: boolean;
  /** The square's side in px. */
  size?: number;
  selected?: boolean;
  /** Drawn by the outline's style (solid, dashed, dotted) as well as its colour. */
  status?: DependencyStatus;
}

const STATUS_CLASS: Record<DependencyStatus, string> = {
  conform: styles.tileConform,
  divergence: styles.tileDivergence,
  absence: styles.tileAbsence,
};

/**
 * One class, drawn the same way everywhere: graph nodes, the module panel, recommendation cards.
 * Not focusable itself; wrap it in a button when it is interactive.
 *
 * @example <ClassTile name="CatalogController" reference size={28} />
 */
export function ClassTile({
  name,
  reference = false,
  size = 48,
  selected = false,
  status,
}: ClassTileProps) {
  const tileClass = [
    styles.tile,
    reference && styles.tileReference,
    selected && styles.tileSelected,
    status && STATUS_CLASS[status],
  ].filter(Boolean);
  return (
    <figure className={styles.tileWrap}>
      <span data-tile className={tileClass.join(' ')} style={{ width: size, height: size }}>
        {reference ? (
          <Mark size={Math.round(size * 0.56)} variant="mono" />
        ) : (
          <TileCode size={size} />
        )}
      </span>
      {name && (
        <figcaption className={reference ? styles.tileNameReference : styles.tileName}>
          {name}
        </figcaption>
      )}
      {reference && <span className={styles.visuallyHidden}>reference class</span>}
      {status && <span className={styles.visuallyHidden}>{CLASS_STATUS_WORDS[status]}</span>}
    </figure>
  );
}

function TileCode({ size }: { size: number }) {
  return (
    <span className={styles.tileCode} style={{ fontSize: Math.max(9, size * 0.26) }} aria-hidden>
      {'</>'}
    </span>
  );
}

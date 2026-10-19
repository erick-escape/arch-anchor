import { Button } from 'antd';
import { memo, useState } from 'react';
import { Meter } from '../../../components/vocabulary/Meter';
import { PackageChip } from '../../../components/vocabulary/PackageChip';
import { PriorityBadge } from '../../../components/vocabulary/PriorityBadge';
import { priorityOf } from '../../../utils/priority';
import { useGraphEmphasisActions } from '../graphEmphasisContext';
import type { RecommendationCardModel } from './recommendationCards';
import { cardDomIdOf } from './cardDomId';
import styles from './RecommendationsPanel.module.css';
import type { CardToApply } from './useApplyRecommendation';

interface RecommendationCardViewProps {
  card: RecommendationCardModel;
  onApply: (toApply: CardToApply) => void;
}

/**
 * One recommendation: a small drawing of the change, its title and description, its priority
 * and rate, the details on request, and Apply, which asks first. Hovering or focusing it
 * highlights on the graph the modules it touches.
 */
export const RecommendationCardView = memo(function RecommendationCardView({
  card,
  onApply,
}: RecommendationCardViewProps) {
  const [open, setOpen] = useState(false);
  const { highlight } = useGraphEmphasisActions();
  return (
    <article
      id={cardDomIdOf(card.id)}
      tabIndex={-1}
      aria-label={card.title}
      className={styles.card}
      onMouseEnter={() => highlight(card.touched)}
      onMouseLeave={() => highlight([])}
      onFocus={() => highlight(card.touched)}
      onBlur={() => highlight([])}
    >
      <div className={styles.cardHead}>
        <span aria-hidden className={`${styles.glyph} ${styles[card.type]}`} />
        <div className={styles.cardText}>
          <h3 className={styles.cardTitle}>
            <BreakAfterSlashes text={card.title} />
          </h3>
          <p className={styles.cardDescription}>{card.description}</p>
        </div>
        {card.rate !== null && <PriorityBadge level={priorityOf(card.rate)} rate={card.rate} />}
      </div>
      {open && <CardDetails card={card} />}
      <div className={styles.cardActions}>
        <Button type="link" size="small" aria-expanded={open} onClick={() => setOpen(!open)}>
          {open ? 'Hide details' : 'Details'}
        </Button>
        <ApplyControl card={card} onApply={onApply} />
      </div>
    </article>
  );
});

function ApplyControl({ card, onApply }: RecommendationCardViewProps) {
  if ('unavailable' in card.apply)
    return <span className={styles.unavailable}>{card.apply.unavailable}</span>;
  const applicable = card.apply;
  return (
    <Button size="small" onClick={() => onApply({ card, applicable })}>
      {applicable.label}
    </Button>
  );
}

function CardDetails({ card }: { card: RecommendationCardModel }) {
  return (
    <div className={styles.details}>
      {card.similarity && (
        <div className={styles.meters}>
          <Meter label={`${card.similarity.of} now`} value={card.similarity.before} />
          <Meter label="after" value={card.similarity.after} />
        </div>
      )}
      {card.violatingTypes && <ViolatingTypes card={card} />}
      <dl className={styles.rows}>
        {card.details.map((row) => (
          <div key={row.label} className={styles.row}>
            <dt>{row.label}</dt>
            <dd>{row.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

function ViolatingTypes({ card }: { card: RecommendationCardModel }) {
  const types = card.violatingTypes ?? [];
  const packageName = card.touched
    .find((id) => id.startsWith('package:'))
    ?.slice('package:'.length);
  return (
    <div className={styles.violating}>
      {packageName && <PackageChip name={packageName} status="divergence" />}
      <span className={styles.violatingCount}>
        {types.length} violating {types.length === 1 ? 'dependency' : 'dependencies'}
      </span>
      <span className={styles.violatingTypes}>
        {types.map((type) => type.split('.').pop()).join(' · ')}
      </span>
    </div>
  );
}

// Lets a long module name wrap after a slash rather than inside a word.
function BreakAfterSlashes({ text }: { text: string }) {
  const parts = text.split('/');
  return (
    <>
      {parts.map((part, index) => (
        <span key={index}>
          {part}
          {index < parts.length - 1 && (
            <>
              /<wbr />
            </>
          )}
        </span>
      ))}
    </>
  );
}

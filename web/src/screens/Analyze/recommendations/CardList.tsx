import { Button } from 'antd';
import { useState } from 'react';
import type { RecommendationCardModel } from './recommendationCards';
import { RecommendationCardView } from './RecommendationCardView';
import styles from './RecommendationsPanel.module.css';
import type { CardToApply } from './useApplyRecommendation';

// Cards drawn at a time. Drawing every card at once froze the page for about 40 s on a project
// with 1,593 recommendations (#49). The paper's evaluation harness pages the same way.
export const CARDS_PER_PAGE = 50;

interface CardListProps {
  cards: RecommendationCardModel[];
  whenEmpty: { title: string; text: string };
  onApply: (toApply: CardToApply) => void;
  // Cards per page, CARDS_PER_PAGE unless given; tests page through a few cards (#54).
  pageSize?: number;
}

/**
 * The cards of one tab, a page at a time: the first page, then the next on request.
 *
 * @example <CardList cards={violations} whenEmpty={noViolations} onApply={confirm} />
 */
export function CardList({ cards, whenEmpty, onApply, pageSize = CARDS_PER_PAGE }: CardListProps) {
  const [shown, setShown] = useState(pageSize);
  if (cards.length === 0) {
    return (
      <div className={styles.empty}>
        <span className={styles.emptyTitle}>{whenEmpty.title}</span>
        <span className={styles.emptyText}>{whenEmpty.text}</span>
      </div>
    );
  }
  const left = cards.length - shown;
  return (
    <div className={styles.cards}>
      {cards.slice(0, shown).map((card) => (
        <RecommendationCardView key={card.id} card={card} onApply={onApply} />
      ))}
      {left > 0 && (
        <Button block onClick={() => setShown(shown + pageSize)}>
          Show {Math.min(left, pageSize)} more of {left}
        </Button>
      )}
    </div>
  );
}

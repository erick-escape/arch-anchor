import { useState } from 'react';
import { Button } from 'antd';
import { UnifiedRecommendation } from '../../interface/Recommendations';
import RecommendationCard from './RecommendationCard';

// Cards drawn at a time. Drawing every card at once froze the page for about 40 s on a project
// with 1,593 recommendations (#49).
export const CARDS_PER_PAGE = 50;

interface CardListProps {
  cards: UnifiedRecommendation[];
  whenEmpty: string;
  onApply: (card: UnifiedRecommendation) => void;
  applyingId: string | null;
}

/**
 * The cards of one tab, a page at a time: the first page, then the next on request.
 *
 * @example
 * <CardList cards={violations} whenEmpty="No violations" onApply={apply} applyingId={null} />
 */
const CardList = ({ cards, whenEmpty, onApply, applyingId }: CardListProps) => {
  const [shown, setShown] = useState(CARDS_PER_PAGE);

  if (cards.length === 0) {
    return <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>{whenEmpty}</div>;
  }

  const left = cards.length - shown;
  return (
    <>
      {cards.slice(0, shown).map((card) => (
        <RecommendationCard
          key={card.id}
          recommendation={card}
          onApply={() => onApply(card)}
          isApplying={applyingId === card.id}
        />
      ))}
      {left > 0 && (
        <Button block onClick={() => setShown(shown + CARDS_PER_PAGE)}>
          Show {Math.min(left, CARDS_PER_PAGE)} more of {left}
        </Button>
      )}
    </>
  );
};

export default CardList;

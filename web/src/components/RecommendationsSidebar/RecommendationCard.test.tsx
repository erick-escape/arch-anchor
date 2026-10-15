// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RecommendationCard from './RecommendationCard';
import { UnifiedRecommendation } from '../../interface/Recommendations';
import {
  mergeRecommendationFixture,
  moveRecommendationFixture,
} from '../../test/fixtures/recommendationFixtures';

const move: UnifiedRecommendation = {
  id: 'move-0',
  type: 'move',
  rating: 0.5625,
  title: 'Move "SalesReport"',
  description: 'Move class with 0.56 improvement',
  data: moveRecommendationFixture(),
};

const merge: UnifiedRecommendation = {
  id: 'merge-0',
  type: 'merge',
  rating: 0.4196,
  title: 'Merge "bookstore/persistence" and "bookstore/web"',
  description: 'Merge modules with 0.42 improvement',
  data: mergeRecommendationFixture(),
};

// The detail line whose label is `label`, label included.
const detailLine = (label: string): HTMLElement | null => screen.getByText(label).parentElement;

describe('RecommendationCard', () => {
  it('names the source and target modules of a move, not their ids', async () => {
    render(<RecommendationCard recommendation={move} onApply={() => {}} isApplying={false} />);

    await userEvent.click(screen.getByText('View Details'));

    expect(screen.getByText('shop/web')).toBeInTheDocument();
    expect(screen.getByText('shop/persistence')).toBeInTheDocument();
    expect(screen.queryByText('m1')).not.toBeInTheDocument();
  });

  // #26: the card showed the reference classes' agreement, 100.0 % for any module with one
  // reference class, so the lines above the improvement did not add up to it.
  it('shows the module similarities a merge improvement is computed from', async () => {
    render(<RecommendationCard recommendation={merge} onApply={() => {}} isApplying={false} />);

    await userEvent.click(screen.getByText('View Details'));

    expect(detailLine('Module 2 Similarity:')).toHaveTextContent('Module 2 Similarity: 75.0%');
    expect(detailLine('Merged Similarity:')).toHaveTextContent('Merged Similarity: 71.4%');
  });

  it('prints a loss of similarity with a minus sign, not "+-"', async () => {
    render(<RecommendationCard recommendation={merge} onApply={() => {}} isApplying={false} />);

    await userEvent.click(screen.getByText('View Details'));

    expect(detailLine('Similarity Improvement:')).toHaveTextContent(
      'Similarity Improvement: \u221216.1%'
    );
  });

  it('still prints a gain with a plus sign', async () => {
    render(<RecommendationCard recommendation={move} onApply={() => {}} isApplying={false} />);

    await userEvent.click(screen.getByText('View Details'));

    expect(detailLine('Similarity Improvement:')).toHaveTextContent(
      'Similarity Improvement: +12.5%'
    );
  });
});

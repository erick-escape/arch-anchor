// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RecommendationCard from './RecommendationCard';
import { UnifiedRecommendation } from '../../interface/Recommendations';
import { moveRecommendationFixture } from '../../test/fixtures/recommendationFixtures';

const move: UnifiedRecommendation = {
  id: 'move-0',
  type: 'move',
  rating: 0.5625,
  title: 'Move "SalesReport"',
  description: 'Move class with 0.56 improvement',
  data: moveRecommendationFixture(),
};

describe('RecommendationCard', () => {
  it('names the source and target modules of a move, not their ids', async () => {
    render(<RecommendationCard recommendation={move} onApply={() => {}} isApplying={false} />);

    await userEvent.click(screen.getByText('View Details'));

    expect(screen.getByText('shop/web')).toBeInTheDocument();
    expect(screen.getByText('shop/persistence')).toBeInTheDocument();
    expect(screen.queryByText('m1')).not.toBeInTheDocument();
  });
});

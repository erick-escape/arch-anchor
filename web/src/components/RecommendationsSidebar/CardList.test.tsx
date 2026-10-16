// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CardList from './CardList';
import { moveCardFixture } from '../../test/fixtures/recommendationFixtures';

// Pages of 2, not the panel's 50: paging through 120 cards took the sidebar's test 10-17 s
// under jsdom, against a 15 s timeout (#54).
const PAGE_SIZE = 2;

function renderList(cardCount: number) {
  const cards = Array.from({ length: cardCount }, (_, index) => moveCardFixture(`Report${index}`));
  render(
    <CardList
      cards={cards}
      whenEmpty="No refactorings recommended"
      onApply={() => {}}
      applyingId={null}
      pageSize={PAGE_SIZE}
    />
  );
}

const applyButtons = (): HTMLElement[] =>
  screen.getAllByRole('button', { name: 'Apply Recommendation' });

describe('CardList', () => {
  it('draws the first page of cards', () => {
    renderList(5);

    expect(applyButtons()).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Show 2 more of 3' })).toBeInTheDocument();
  });

  it('draws the next page on request, then the rest', async () => {
    renderList(5);

    await userEvent.click(screen.getByRole('button', { name: 'Show 2 more of 3' }));
    expect(applyButtons()).toHaveLength(4);

    await userEvent.click(screen.getByRole('button', { name: 'Show 1 more of 1' }));
    expect(applyButtons()).toHaveLength(5);
    expect(screen.queryByRole('button', { name: /Show .* more/ })).not.toBeInTheDocument();
  });

  it('offers no more when every card fits on the first page', () => {
    renderList(2);

    expect(applyButtons()).toHaveLength(2);
    expect(screen.queryByRole('button', { name: /Show .* more/ })).not.toBeInTheDocument();
  });

  it('says so when there are no cards', () => {
    renderList(0);

    expect(screen.getByText('No refactorings recommended')).toBeInTheDocument();
  });
});

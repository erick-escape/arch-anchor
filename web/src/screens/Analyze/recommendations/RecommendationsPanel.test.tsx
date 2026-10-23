// @vitest-environment jsdom
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { type AxiosRequestConfig } from 'axios';
import { useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { FakeAxiosAdapter } from '../../../test/fakes/FakeAxiosAdapter';
import {
  mergeRecommendationFixture,
  moveRecommendationFixture,
  recommendationsFixture,
  splitRecommendationFixture,
  violationRecommendationFixture,
} from '../../../test/fixtures/recommendationFixtures';
import { renderWithProviders } from '../../../test/renderWithProviders';
import { GraphEmphasisProvider } from '../graphEmphasis';
import { useGraphEmphasisState } from '../graphEmphasisContext';
import { CardList } from './CardList';
import { refactoringCardsOf } from './recommendationCards';
import { RecommendationsPanel, type RecommendationsTab } from './RecommendationsPanel';

// The page holds the tab shown, so the command palette can open a card.
function PanelWithTabs({ onApplied }: { onApplied: () => void }) {
  const [tab, setTab] = useState<RecommendationsTab>('refactorings');
  return (
    <RecommendationsPanel
      projectName="shop"
      analyzed
      onApplied={onApplied}
      tab={tab}
      onTabChange={setTab}
    />
  );
}

// Shows what the graph is asked to highlight, as the canvas would draw it.
function HighlightProbe() {
  const { highlighted } = useGraphEmphasisState();
  return <output aria-label="highlighted">{[...highlighted].join(' ')}</output>;
}

describe('RecommendationsPanel', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];
  let onApplied: ReturnType<typeof vi.fn<() => void>>;

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter();
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
    onApplied = vi.fn<() => void>();
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  function renderPanel() {
    return renderWithProviders(
      <GraphEmphasisProvider>
        <PanelWithTabs onApplied={onApplied} />
        <HighlightProbe />
      </GraphEmphasisProvider>
    );
  }

  function bookstoreCards() {
    fakeApi.respondTo(
      'GET',
      '/api/recommendations',
      recommendationsFixture({
        splits: [splitRecommendationFixture()],
        merges: [mergeRecommendationFixture()],
        moves: [moveRecommendationFixture()],
        violations: [violationRecommendationFixture()],
      })
    );
  }

  it('counts the cards of each tab, and the total, from the same list', async () => {
    bookstoreCards();
    renderPanel();

    expect(await screen.findByRole('tab', { name: /Refactorings\s*3/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Violations\s*1/ })).toBeInTheDocument();
    expect(screen.getByText('4 in total')).toBeInTheDocument();
    expect(screen.getByLabelText('High priority')).toHaveTextContent('2');
    expect(screen.getByLabelText('Medium priority')).toHaveTextContent('1');
    expect(screen.getByLabelText('Low priority')).toHaveTextContent('0');
  });

  it('lists the refactorings by rate, the split first, and the violation in its own tab', async () => {
    bookstoreCards();
    renderPanel();

    const titles = (await screen.findAllByRole('article')).map((card) =>
      card.getAttribute('aria-label')
    );
    expect(titles).toEqual([
      'Split “shop/web”',
      'Move “SalesReport”',
      'Merge “bookstore/persistence” and “bookstore/web”',
    ]);
    await userEvent.click(screen.getByRole('tab', { name: /Violations/ }));
    expect(
      await screen.findByRole('article', { name: 'Fix violation in “SalesReport”' })
    ).toBeInTheDocument();
  });

  it('highlights on the graph the modules a hovered card touches', async () => {
    bookstoreCards();
    renderPanel();

    await userEvent.hover(await screen.findByRole('article', { name: 'Move “SalesReport”' }));

    expect(screen.getByLabelText('highlighted')).toHaveTextContent('m1 m2');
  });

  it('applies a recommendation only after confirming it, then reloads the modules', async () => {
    fakeApi
      .respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ moves: [moveRecommendationFixture()] })
      )
      .respondTo('POST', '/api/recommendations/apply/move', {});
    renderPanel();

    await userEvent.click(await screen.findByRole('button', { name: 'Apply move…' }));
    expect(fakeApi.requestsTo('POST', '/api/recommendations/apply/move')).toHaveLength(0);
    const dialog = within(await screen.findByRole('dialog'));
    expect(dialog.getByText('Move SalesReport to shop/persistence?')).toBeInTheDocument();
    await userEvent.click(dialog.getByRole('button', { name: 'Apply move' }));

    await vi.waitFor(() => expect(onApplied).toHaveBeenCalled());
    expect(fakeApi.requestsTo('POST', '/api/recommendations/apply/move')[0].params).toEqual({
      classId: 'c9',
      sourceModuleId: 'm1',
      targetModuleId: 'm2',
    });
    expect(await screen.findByText('Moved SalesReport to shop/persistence')).toBeInTheDocument();
  });

  it('says what went wrong when applying fails', async () => {
    fakeApi
      .respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ splits: [splitRecommendationFixture()] })
      )
      .failWith('POST', '/api/recommendations/apply/split', 500, {});
    renderPanel();

    await userEvent.click(await screen.findByRole('button', { name: 'Apply split…' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Apply split' }));

    expect(await screen.findByText(/Could not apply the split/)).toBeInTheDocument();
    expect(onApplied).not.toHaveBeenCalled();
  });

  it('shows a violation’s offending types, and why nothing can be applied when no module fits', async () => {
    const unfit = violationRecommendationFixture({
      bestSuggestion: {
        ...violationRecommendationFixture().bestSuggestion!,
        requiresNewModule: true,
      },
    });
    fakeApi.respondTo(
      'GET',
      '/api/recommendations',
      recommendationsFixture({ violations: [unfit] })
    );
    renderPanel();

    await userEvent.click(await screen.findByRole('tab', { name: /Violations/ }));
    const card = within(
      await screen.findByRole('article', { name: 'Fix violation in “SalesReport”' })
    );
    expect(card.getByText(/No existing module fits SalesReport/)).toBeInTheDocument();
    await userEvent.click(card.getByRole('button', { name: 'Details' }));
    expect(card.getByText('1 violating dependency')).toBeInTheDocument();
    expect(card.getByText('Connection')).toBeInTheDocument();
  });

  it('says when there is nothing to recommend', async () => {
    fakeApi.respondTo('GET', '/api/recommendations', recommendationsFixture());
    renderPanel();

    expect(await screen.findByText('No refactorings recommended')).toBeInTheDocument();
  });

  it('says why the recommendations are missing and offers to try again', async () => {
    fakeApi.failWith('GET', '/api/recommendations', 500, {});
    renderPanel();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not load the recommendations'
    );
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });
});

describe('CardList', () => {
  const cards = refactoringCardsOf(
    recommendationsFixture({
      moves: ['A', 'B', 'C'].map((name) => moveRecommendationFixture({ className: name })),
    })
  );

  it('draws a page of cards, then the next on request (#49)', async () => {
    renderWithProviders(
      <CardList
        cards={cards}
        onApply={() => {}}
        pageSize={2}
        whenEmpty={{ title: 'None', text: '' }}
      />
    );

    expect(screen.getAllByRole('article')).toHaveLength(2);
    await userEvent.click(screen.getByRole('button', { name: 'Show 1 more of 1' }));
    expect(screen.getAllByRole('article')).toHaveLength(3);
    expect(screen.queryByRole('button', { name: /Show .* more/ })).toBeNull();
  });

  it('draws 50 cards a page by default, as the paper’s evaluation harness counts them', () => {
    const many = refactoringCardsOf(
      recommendationsFixture({
        moves: Array.from({ length: 120 }, (_, index) =>
          moveRecommendationFixture({ className: `C${index}` })
        ),
      })
    );
    renderWithProviders(
      <CardList cards={many} onApply={() => {}} whenEmpty={{ title: 'None', text: '' }} />
    );

    expect(screen.getAllByRole('article')).toHaveLength(50);
    expect(screen.getByRole('button', { name: 'Show 50 more of 70' })).toBeInTheDocument();
  });
});

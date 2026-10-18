import { Button, Spin, Tabs } from 'antd';
import { useMemo, useState } from 'react';
import { ConfirmDialog } from '../../../components/ConfirmDialog/ConfirmDialog';
import { PriorityBadge } from '../../../components/vocabulary/PriorityBadge';
import { useRecommendations } from '../../../hooks/useRecommendations';
import type { RecommendationsResponse } from '../../../interface/Recommendations';
import { priorityOf, type PriorityLabel } from '../../../utils/priority';
import { CardList } from './CardList';
import {
  refactoringCardsOf,
  violationCardsOf,
  type RecommendationCardModel,
} from './recommendationCards';
import styles from './RecommendationsPanel.module.css';
import { useApplyRecommendation, type CardToApply } from './useApplyRecommendation';

interface RecommendationsPanelProps {
  projectName: string;
  /** The project's analysis has returned, so the API's recommendations are this project's. */
  analyzed: boolean;
  /** Reloads the modules the API saved after applying a change. */
  onApplied: () => void;
}

/**
 * The recommendations, floating over the graph's right edge: the refactorings by rate and the
 * violations in a tab of their own (#48). Apply asks first, then the change lands on the graph.
 *
 * @example <RecommendationsPanel projectName="bookstore" analyzed onApplied={reload} />
 */
export function RecommendationsPanel({
  projectName,
  analyzed,
  onApplied,
}: RecommendationsPanelProps) {
  const query = useRecommendations(projectName, analyzed);
  const [toApply, setToApply] = useState<CardToApply | null>(null);
  const apply = useApplyRecommendation(onApplied);
  const confirm = () => toApply && apply.mutate(toApply, { onSettled: () => setToApply(null) });
  return (
    <aside aria-label="Recommendations" className={styles.panel}>
      {query.isPending && <Rating />}
      {query.isError && <LoadFailed onRetry={() => query.refetch()} />}
      {query.data && <RecommendationTabs response={query.data} onApply={setToApply} />}
      <ConfirmDialog
        open={toApply !== null}
        title={toApply?.applicable.confirmTitle ?? ''}
        confirmLabel={toApply?.applicable.confirmLabel ?? ''}
        busy={apply.isPending}
        onConfirm={confirm}
        onCancel={() => setToApply(null)}
      >
        {toApply?.applicable.confirmBody}
      </ConfirmDialog>
    </aside>
  );
}

function RecommendationTabs({
  response,
  onApply,
}: {
  response: RecommendationsResponse;
  onApply: (toApply: CardToApply) => void;
}) {
  const refactorings = useMemo(() => refactoringCardsOf(response), [response]);
  const violations = useMemo(() => violationCardsOf(response), [response]);
  const items = [
    {
      key: 'refactorings',
      label: `Refactorings ${refactorings.length}`,
      children: (
        <>
          <PrioritySummary cards={refactorings} />
          <CardList
            cards={refactorings}
            onApply={onApply}
            whenEmpty={{
              title: 'No refactorings recommended',
              text: 'No split, merge or move would improve the modules.',
            }}
          />
        </>
      ),
    },
    {
      key: 'violations',
      label: `Violations ${violations.length}`,
      children: (
        <CardList
          cards={violations}
          onApply={onApply}
          whenEmpty={{
            title: 'No violations',
            text: 'Every dependency is in its module’s contract.',
          }}
        />
      ),
    },
  ];
  return (
    <>
      <div className={styles.head}>
        <h2 className={styles.title}>Recommendations</h2>
        <span className={styles.total}>{refactorings.length + violations.length} in total</span>
      </div>
      <Tabs className={styles.tabs} items={items} />
    </>
  );
}

const LEVELS: PriorityLabel[] = ['High', 'Medium', 'Low'];

function PrioritySummary({ cards }: { cards: RecommendationCardModel[] }) {
  const count = (level: PriorityLabel) =>
    cards.filter((card) => card.rate !== null && priorityOf(card.rate) === level).length;
  return (
    <div className={styles.summary}>
      {LEVELS.map((level) => (
        <span key={level} aria-label={`${level} priority`} className={styles.summaryItem}>
          <PriorityBadge level={level} />
          <b>{count(level)}</b>
        </span>
      ))}
    </div>
  );
}

function Rating() {
  return (
    <div role="status" className={styles.state}>
      <Spin />
      <span>Rating the recommendations</span>
    </div>
  );
}

function LoadFailed({ onRetry }: { onRetry: () => void }) {
  return (
    <div role="alert" className={styles.failure}>
      <span className={styles.failureTitle}>Could not load the recommendations</span>
      <span>Check that the API is running, then try again.</span>
      <Button size="small" onClick={onRetry}>
        Try again
      </Button>
    </div>
  );
}

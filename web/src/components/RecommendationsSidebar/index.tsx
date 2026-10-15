import React, { useState, useMemo } from 'react';
import { Badge, Spin, Alert, Tabs } from 'antd';
import { RightOutlined, LeftOutlined } from '@ant-design/icons';
import {
  useRecommendations,
  useApplySplit,
  useApplyMerge,
  useApplyMove,
} from '../../hooks/useRecommendations';
import {
  UnifiedRecommendation,
  SplitModuleRecommendation,
  MergeModuleRecommendation,
  MoveClassRecommendation,
  ArchitectureViolationRecommendation,
} from '../../interface/Recommendations';
import CardList from './CardList';
import { PRIORITY_COLORS, PriorityLabel, priorityOf } from '../../utils/priority';

const SUMMARY_BOXES = (['High', 'Medium', 'Low'] as const).map((label) => ({
  label,
  color: PRIORITY_COLORS[label],
}));

// A tab's name followed by its number of cards, as plain text, so it is part of the tab's name.
// Light text: the panel is dark, and the inactive tab's default text is dark too (#48); the
// active tab is marked by its underline.
const TabLabel = ({ name, count }: { name: string; count: number }) => (
  <span style={{ color: '#d9d9d9' }}>
    {name} <span style={{ color: '#999' }}>{count}</span>
  </span>
);

interface RecommendationsSidebarProps {
  onRecommendationApplied?: () => void;
}

const RecommendationsSidebar: React.FC<RecommendationsSidebarProps> = ({
  onRecommendationApplied,
}) => {
  const [isOpen, setIsOpen] = useState(true);
  const [applyingId, setApplyingId] = useState<string | null>(null);

  const { data: recommendations, isLoading, error } = useRecommendations();
  const applySplit = useApplySplit();
  const applyMerge = useApplyMerge();
  const applyMove = useApplyMove();

  // Splits, merges and moves have a rate and are listed by it; violations have none and get a
  // tab of their own (#48), instead of a made-up rating among the refactorings.
  const { refactorings, violations } = useMemo(() => {
    if (!recommendations) {
      return { refactorings: [], violations: [] };
    }

    const rated: UnifiedRecommendation[] = [
      ...recommendations.splits.map((split, index) => ({
        id: `split-${index}`,
        type: 'split' as const,
        rating: split.metrics.rate,
        title: `Split "${split.originalModuleName}"`,
        description: `Split into 2 modules with ${split.metrics.rate.toFixed(2)} improvement`,
        data: split,
      })),
      ...recommendations.merges.map((merge, index) => ({
        id: `merge-${index}`,
        type: 'merge' as const,
        rating: merge.metrics.rate,
        title: `Merge "${merge.module1Name}" and "${merge.module2Name}"`,
        description: `Combine modules with ${merge.metrics.rate.toFixed(2)} improvement`,
        data: merge,
      })),
      ...recommendations.moves.map((move, index) => ({
        id: `move-${index}`,
        type: 'move' as const,
        rating: move.metrics.rate,
        title: `Move "${move.className}"`,
        description: `Move class with ${move.metrics.rate.toFixed(2)} improvement`,
        data: move,
      })),
    ];
    // Highest rate first; the sort is stable, so equal rates keep the order above
    rated.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));

    const violationCards: UnifiedRecommendation[] = recommendations.violations.map(
      (violation, index) => ({
        id: `violation-${index}`,
        type: 'violation' as const,
        rating: null,
        title: `Fix violation in "${violation.className}"`,
        description: `Resolve ${violation.violation} dependency violation`,
        data: violation,
      })
    );

    return { refactorings: rated, violations: violationCards };
  }, [recommendations]);

  const priorityCounts = useMemo(() => {
    const counts: Record<PriorityLabel, number> = { High: 0, Medium: 0, Low: 0 };
    refactorings.forEach((card) => {
      counts[priorityOf(card.rating ?? 0).label] += 1;
    });
    return counts;
  }, [refactorings]);

  const handleApply = async (recommendation: UnifiedRecommendation) => {
    setApplyingId(recommendation.id);

    try {
      switch (recommendation.type) {
        case 'split': {
          const data = recommendation.data as SplitModuleRecommendation;
          // Module 2 holds the classes to split off; module 1 keeps the reference class, and
          // with it the original module's id and its place in the graph.
          await applySplit.mutateAsync({
            moduleId: data.originalModuleId,
            classIds: data.module2ClassIds,
          });
          break;
        }
        case 'merge': {
          const data = recommendation.data as MergeModuleRecommendation;
          await applyMerge.mutateAsync({
            sourceId: data.module1Id,
            targetId: data.module2Id,
          });
          break;
        }
        case 'move': {
          const data = recommendation.data as MoveClassRecommendation;
          await applyMove.mutateAsync({
            classId: data.classId,
            sourceModuleId: data.sourceModuleId,
            targetModuleId: data.targetModuleId,
          });
          break;
        }
        case 'violation': {
          const data = recommendation.data as ArchitectureViolationRecommendation;
          if (data.bestSuggestion && !data.bestSuggestion.requiresNewModule) {
            await applyMove.mutateAsync({
              classId: data.classId,
              sourceModuleId: data.sourceModuleId,
              targetModuleId: data.bestSuggestion.targetModuleId,
            });
          }
          break;
        }
      }

      // Refresh the Analyze page UI
      if (onRecommendationApplied) {
        onRecommendationApplied();
      }
    } catch {
      // The mutation's onError has already told the user, and nothing changed to refresh.
    } finally {
      setApplyingId(null);
    }
  };

  const sidebarStyle: React.CSSProperties = {
    position: 'fixed',
    top: '60px',
    right: isOpen ? '0' : '-350px',
    width: '350px',
    height: 'calc(100vh - 60px)',
    backgroundColor: '#000',
    borderLeft: '1px solid #333',
    transition: 'right 0.3s ease',
    zIndex: 100,
    display: 'flex',
    flexDirection: 'column',
  };

  const toggleButtonStyle: React.CSSProperties = {
    position: 'absolute',
    top: '50%',
    left: '-40px',
    transform: 'translateY(-50%)',
    width: '40px',
    height: '80px',
    backgroundColor: '#000',
    border: '1px solid #333',
    borderRight: 'none',
    borderRadius: '8px 0 0 8px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#fff',
    fontSize: '20px',
  };

  const headerStyle: React.CSSProperties = {
    padding: '16px',
    borderBottom: '1px solid #333',
    backgroundColor: '#111',
  };

  const summaryStyle: React.CSSProperties = {
    display: 'flex',
    gap: '12px',
    padding: '12px 16px',
    borderBottom: '1px solid #333',
    backgroundColor: '#0a0a0a',
  };

  const summaryItemStyle = (color: string): React.CSSProperties => ({
    flex: 1,
    textAlign: 'center',
    padding: '8px',
    backgroundColor: '#1a1a1a',
    borderRadius: '4px',
    border: `1px solid ${color}`,
  });

  const contentStyle: React.CSSProperties = {
    flex: 1,
    overflowY: 'auto',
    padding: '16px',
  };

  return (
    <div style={sidebarStyle}>
      <div style={toggleButtonStyle} onClick={() => setIsOpen(!isOpen)}>
        {isOpen ? <RightOutlined /> : <LeftOutlined />}
      </div>

      <div style={headerStyle}>
        <h3 style={{ margin: 0, color: '#fff', fontSize: '18px' }}>
          Recommendations
          {recommendations && (
            <Badge
              count={recommendations.summary.totalRecommendations}
              style={{ marginLeft: '12px', backgroundColor: '#1890ff' }}
            />
          )}
        </h3>
      </div>

      <div style={contentStyle}>
        {isLoading && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Spin size="large" />
          </div>
        )}

        {error && (
          <Alert
            message="Error"
            description="Failed to load recommendations"
            type="error"
            showIcon
          />
        )}

        {recommendations && (
          <Tabs
            items={[
              {
                key: 'refactorings',
                label: <TabLabel name="Refactorings" count={refactorings.length} />,
                children: (
                  <>
                    <div style={summaryStyle}>
                      {SUMMARY_BOXES.map(({ label, color }) => (
                        <div
                          key={label}
                          style={summaryItemStyle(color)}
                          aria-label={`${label} priority`}
                        >
                          <div style={{ fontSize: '20px', fontWeight: 'bold', color }}>
                            {priorityCounts[label]}
                          </div>
                          <div style={{ fontSize: '12px', color: '#999' }}>{label}</div>
                        </div>
                      ))}
                    </div>
                    <CardList
                      cards={refactorings}
                      whenEmpty="No refactorings recommended"
                      onApply={handleApply}
                      applyingId={applyingId}
                    />
                  </>
                ),
              },
              {
                key: 'violations',
                label: <TabLabel name="Violations" count={violations.length} />,
                children: (
                  <CardList
                    cards={violations}
                    whenEmpty="No violations"
                    onApply={handleApply}
                    applyingId={applyingId}
                  />
                ),
              },
            ]}
          />
        )}
      </div>
    </div>
  );
};

export default RecommendationsSidebar;

import React, { useState, useMemo } from 'react';
import { Badge, Spin, Alert } from 'antd';
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
import RecommendationCard from './RecommendationCard';

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

  const unifiedRecommendations = useMemo<UnifiedRecommendation[]>(() => {
    if (!recommendations) return [];

    const unified: UnifiedRecommendation[] = [];

    // Convert split recommendations
    recommendations.splits.forEach((split, index) => {
      unified.push({
        id: `split-${index}`,
        type: 'split',
        rating: split.metrics.rate,
        title: `Split "${split.originalModuleName}"`,
        description: `Split into 2 modules with ${split.metrics.rate.toFixed(2)} improvement`,
        data: split,
      });
    });

    // Convert merge recommendations
    recommendations.merges.forEach((merge, index) => {
      unified.push({
        id: `merge-${index}`,
        type: 'merge',
        rating: merge.metrics.rate,
        title: `Merge "${merge.module1Name}" and "${merge.module2Name}"`,
        description: `Combine modules with ${merge.metrics.rate.toFixed(2)} improvement`,
        data: merge,
      });
    });

    // Convert move recommendations
    recommendations.moves.forEach((move, index) => {
      unified.push({
        id: `move-${index}`,
        type: 'move',
        rating: move.metrics.rate,
        title: `Move "${move.className}"`,
        description: `Move class with ${move.metrics.rate.toFixed(2)} improvement`,
        data: move,
      });
    });

    // Convert violation recommendations (default medium priority = 0.3)
    recommendations.violations.forEach((violation, index) => {
      unified.push({
        id: `violation-${index}`,
        type: 'violation',
        rating: 0.3,
        title: `Fix violation in "${violation.className}"`,
        description: `Resolve ${violation.violation} dependency violation`,
        data: violation,
      });
    });

    // Sort by rating descending
    return unified.sort((a, b) => b.rating - a.rating);
  }, [recommendations]);

  const handleApply = async (recommendation: UnifiedRecommendation) => {
    setApplyingId(recommendation.id);

    try {
      switch (recommendation.type) {
        case 'split': {
          const data = recommendation.data as SplitModuleRecommendation;
          await applySplit.mutateAsync({
            moduleId: data.originalModuleId,
            classIds: data.module1ClassIds,
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

      {recommendations && (
        <div style={summaryStyle}>
          <div style={summaryItemStyle('#ff4d4f')}>
            <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#ff4d4f' }}>
              {recommendations.summary.highPriority}
            </div>
            <div style={{ fontSize: '12px', color: '#999' }}>High</div>
          </div>
          <div style={summaryItemStyle('#faad14')}>
            <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#faad14' }}>
              {recommendations.summary.mediumPriority}
            </div>
            <div style={{ fontSize: '12px', color: '#999' }}>Medium</div>
          </div>
          <div style={summaryItemStyle('#52c41a')}>
            <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#52c41a' }}>
              {recommendations.summary.lowPriority}
            </div>
            <div style={{ fontSize: '12px', color: '#999' }}>Low</div>
          </div>
        </div>
      )}

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

        {!isLoading && !error && unifiedRecommendations.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            No recommendations available
          </div>
        )}

        {unifiedRecommendations.map((recommendation) => (
          <RecommendationCard
            key={recommendation.id}
            recommendation={recommendation}
            onApply={() => handleApply(recommendation)}
            isApplying={applyingId === recommendation.id}
          />
        ))}
      </div>
    </div>
  );
};

export default RecommendationsSidebar;

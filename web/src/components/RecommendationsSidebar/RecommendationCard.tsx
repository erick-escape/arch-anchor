import React from 'react';
import { Button, Tag, Collapse } from 'antd';
import {
  SplitCellsOutlined,
  MergeCellsOutlined,
  ArrowRightOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import {
  UnifiedRecommendation,
  SplitModuleRecommendation,
  MergeModuleRecommendation,
  MoveClassRecommendation,
  ArchitectureViolationRecommendation,
} from '../../interface/Recommendations';

const { Panel } = Collapse;

interface RecommendationCardProps {
  recommendation: UnifiedRecommendation;
  onApply: () => void;
  isApplying: boolean;
}

const RecommendationCard: React.FC<RecommendationCardProps> = ({
  recommendation,
  onApply,
  isApplying,
}) => {
  const getPriorityColor = (rating: number): string => {
    if (rating > 0.5) return '#ff4d4f'; // red
    if (rating > 0.2) return '#faad14'; // orange
    return '#52c41a'; // green
  };

  const getPriorityLabel = (rating: number): string => {
    if (rating > 0.5) return 'High';
    if (rating > 0.2) return 'Medium';
    return 'Low';
  };

  const getIcon = () => {
    switch (recommendation.type) {
      case 'split':
        return <SplitCellsOutlined style={{ fontSize: '20px' }} />;
      case 'merge':
        return <MergeCellsOutlined style={{ fontSize: '20px' }} />;
      case 'move':
        return <ArrowRightOutlined style={{ fontSize: '20px' }} />;
      case 'violation':
        return <WarningOutlined style={{ fontSize: '20px' }} />;
    }
  };

  const renderDetails = () => {
    switch (recommendation.type) {
      case 'split':
        return renderSplitDetails(recommendation.data as SplitModuleRecommendation);
      case 'merge':
        return renderMergeDetails(recommendation.data as MergeModuleRecommendation);
      case 'move':
        return renderMoveDetails(recommendation.data as MoveClassRecommendation);
      case 'violation':
        return renderViolationDetails(recommendation.data as ArchitectureViolationRecommendation);
    }
  };

  const renderSplitDetails = (data: SplitModuleRecommendation) => (
    <div style={{ fontSize: '12px', color: '#999' }}>
      <p>
        <strong>Original Similarity:</strong> {(data.metrics.originalSimilarity * 100).toFixed(1)}%
      </p>
      <p>
        <strong>After Split (Avg):</strong> {(data.metrics.avgSplitSimilarity * 100).toFixed(1)}%
      </p>
      <p>
        <strong>Similarity Improvement:</strong> +
        {(data.metrics.similarityImprovement * 100).toFixed(1)}%
      </p>
      <p>
        <strong>Violations:</strong> {data.metrics.originalViolations} →{' '}
        {data.metrics.totalSplitViolations}
      </p>
      <p>
        <strong>Module 1 Classes:</strong> {data.module1ClassIds.length}
      </p>
      <p>
        <strong>Module 2 Classes:</strong> {data.module2ClassIds.length}
      </p>
    </div>
  );

  const renderMergeDetails = (data: MergeModuleRecommendation) => (
    <div style={{ fontSize: '12px', color: '#999' }}>
      <p>
        <strong>Module 1 Similarity:</strong> {(data.metrics.module1Similarity * 100).toFixed(1)}%
      </p>
      <p>
        <strong>Module 2 Similarity:</strong> {(data.metrics.module2Similarity * 100).toFixed(1)}%
      </p>
      <p>
        <strong>Merged Similarity:</strong> {(data.metrics.mergedSimilarity * 100).toFixed(1)}%
      </p>
      <p>
        <strong>Similarity Improvement:</strong> +
        {(data.metrics.similarityImprovement * 100).toFixed(1)}%
      </p>
      <p>
        <strong>Violations:</strong>{' '}
        {data.metrics.module1Violations + data.metrics.module2Violations} →{' '}
        {data.metrics.mergedViolations}
      </p>
    </div>
  );

  const renderMoveDetails = (data: MoveClassRecommendation) => (
    <div style={{ fontSize: '12px', color: '#999' }}>
      <p>
        <strong>From Module:</strong> {data.sourceModuleId}
      </p>
      <p>
        <strong>To Module:</strong> {data.targetModuleId}
      </p>
      <p>
        <strong>Similarity Improvement:</strong> +
        {(data.metrics.similarityImprovement * 100).toFixed(1)}%
      </p>
      <p>
        <strong>Source Violations:</strong> {data.metrics.sourceViolationsBefore} →{' '}
        {data.metrics.sourceViolationsAfter}
      </p>
      <p>
        <strong>Target Violations:</strong> {data.metrics.targetViolationsBefore} →{' '}
        {data.metrics.targetViolationsAfter}
      </p>
    </div>
  );

  const renderViolationDetails = (data: ArchitectureViolationRecommendation) => (
    <div style={{ fontSize: '12px', color: '#999' }}>
      <p>
        <strong>Source Module:</strong> {data.sourceModuleName}
      </p>
      <p>
        <strong>Violation:</strong> {data.violation}
      </p>
      <p>
        <strong>Violating Dependencies:</strong> {data.violatingDependencies.length}
      </p>
      {data.bestSuggestion && (
        <>
          <p>
            <strong>Suggested Target:</strong> {data.bestSuggestion.targetModuleName}
          </p>
          <p>
            <strong>Similarity Improvement:</strong> +
            {(data.bestSuggestion.similarityImprovement * 100).toFixed(1)}%
          </p>
          <p>
            <strong>New Violations:</strong> {data.bestSuggestion.newViolationsCreated}
          </p>
        </>
      )}
    </div>
  );

  const priorityColor = getPriorityColor(recommendation.rating);
  const priorityLabel = getPriorityLabel(recommendation.rating);

  return (
    <div
      style={{
        backgroundColor: '#1a1a1a',
        border: `2px solid ${priorityColor}`,
        borderRadius: '8px',
        padding: '12px',
        marginBottom: '12px',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
        <div style={{ color: priorityColor }}>{getIcon()}</div>
        <div style={{ flex: 1 }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              marginBottom: '8px',
            }}
          >
            <div>
              <div style={{ fontWeight: 'bold', color: '#fff', marginBottom: '4px' }}>
                {recommendation.title}
              </div>
              <div style={{ fontSize: '12px', color: '#999' }}>{recommendation.description}</div>
            </div>
            <Tag color={priorityColor}>{priorityLabel}</Tag>
          </div>

          <Collapse ghost bordered={false} style={{ backgroundColor: 'transparent' }}>
            <Panel
              header={<span style={{ color: '#1890ff', fontSize: '12px' }}>View Details</span>}
              key="1"
              style={{ border: 'none' }}
            >
              {renderDetails()}
            </Panel>
          </Collapse>

          <Button
            type="primary"
            size="small"
            onClick={onApply}
            loading={isApplying}
            style={{ marginTop: '8px', width: '100%' }}
          >
            Apply Recommendation
          </Button>
        </div>
      </div>
    </div>
  );
};

export default RecommendationCard;

export type PriorityLabel = 'High' | 'Medium' | 'Low';

export const PRIORITY_COLORS: Record<PriorityLabel, string> = {
  High: '#ff4d4f',
  Medium: '#faad14',
  Low: '#52c41a',
};

export interface Priority {
  label: PriorityLabel;
  color: string;
}

/**
 * The priority of a refactoring by its rate, with the backend's thresholds
 * (RecommendationsService): above 0.5 High, above 0.2 Medium, Low otherwise. Violations have no
 * rate and so no priority (#48).
 *
 * @example
 * priorityOf(0.5625); // { label: 'High', color: '#ff4d4f' }
 */
export function priorityOf(rate: number): Priority {
  const label: PriorityLabel = rate > 0.5 ? 'High' : rate > 0.2 ? 'Medium' : 'Low';
  return { label, color: PRIORITY_COLORS[label] };
}

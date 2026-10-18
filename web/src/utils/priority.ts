export type PriorityLabel = 'High' | 'Medium' | 'Low';

/**
 * The priority of a refactoring by its rate, with the backend's thresholds
 * (RecommendationsService): above 0.5 High, above 0.2 Medium, Low otherwise. Violations have no
 * rate and so no priority (#48). Drawn by PriorityBadge in brand intensity, never in the colours
 * that mean dependency status.
 *
 * @example
 * priorityOf(0.5625); // 'High'
 */
export function priorityOf(rate: number): PriorityLabel {
  return rate > 0.5 ? 'High' : rate > 0.2 ? 'Medium' : 'Low';
}

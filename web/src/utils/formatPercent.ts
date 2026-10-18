/**
 * A similarity as the tool has always printed it: a percentage with two decimals.
 *
 * @example formatPercent(0.8333) // '83.33%'
 */
export function formatPercent(fraction: number): string {
  return `${(fraction * 100).toFixed(2)}%`;
}

/**
 * A recommendation rate, printed with two decimals wherever it appears (cards, badges, dialogs).
 *
 * @example formatRate(0.5625) // '0.56'
 */
export function formatRate(rate: number): string {
  return rate.toFixed(2);
}

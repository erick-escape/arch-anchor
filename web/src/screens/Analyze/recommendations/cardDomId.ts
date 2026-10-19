/**
 * The DOM id of a recommendation's card, so the palette can scroll to it and focus it.
 *
 * @example cardDomIdOf('move:c9:m2'); // 'recommendation-move-c9-m2'
 */
export function cardDomIdOf(cardId: string): string {
  return `recommendation-${cardId.replace(/[^A-Za-z0-9_-]/g, '-')}`;
}

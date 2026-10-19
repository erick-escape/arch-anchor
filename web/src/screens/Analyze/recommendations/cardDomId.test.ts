import { describe, expect, it } from 'vitest';
import { cardDomIdOf } from './cardDomId';

describe('cardDomIdOf', () => {
  it('turns a card id into a DOM id with no characters a selector would trip on', () => {
    expect(cardDomIdOf('move:c9:m2')).toBe('recommendation-move-c9-m2');
    expect(cardDomIdOf('split:m1')).toBe('recommendation-split-m1');
  });
});

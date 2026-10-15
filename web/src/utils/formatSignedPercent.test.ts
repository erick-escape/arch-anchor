import { describe, expect, it } from 'vitest';
import { formatSignedPercent } from './formatSignedPercent';

describe('formatSignedPercent', () => {
  it('prints a gain with a plus sign, as the cards always have', () => {
    expect(formatSignedPercent(0.125)).toBe('+12.5%');
  });

  // #26: the bookstore's merge card read "+-16.1%".
  it('prints a loss with a minus sign and no plus', () => {
    expect(formatSignedPercent(-0.1607)).toBe('−16.1%');
  });

  it('prints no sign for a change that rounds to zero', () => {
    expect(formatSignedPercent(0)).toBe('0.0%');
    expect(formatSignedPercent(-0.0004)).toBe('0.0%');
  });
});

import { describe, expect, it } from 'vitest';
import { formatPercent, formatRate } from './formatPercent';

describe('formatPercent', () => {
  it('prints a fraction as a percentage with two decimals, as the bookstore README quotes it', () => {
    expect(formatPercent(0.8333)).toBe('83.33%');
    expect(formatPercent(1)).toBe('100.00%');
    expect(formatPercent(0.5)).toBe('50.00%');
  });
});

describe('formatRate', () => {
  it('prints a rate with two decimals', () => {
    expect(formatRate(0.5625)).toBe('0.56');
    expect(formatRate(0.625)).toBe('0.63');
  });
});

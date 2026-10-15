import { describe, expect, it } from 'vitest';
import { priorityOf } from './priority';

describe('priorityOf', () => {
  it('uses the backend thresholds, both exclusive', () => {
    expect(priorityOf(0.51).label).toBe('High');
    expect(priorityOf(0.5).label).toBe('Medium');
    expect(priorityOf(0.21).label).toBe('Medium');
    expect(priorityOf(0.2).label).toBe('Low');
  });
});

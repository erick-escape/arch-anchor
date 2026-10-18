// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { prefersReducedMotion } from './prefersReducedMotion';

describe('prefersReducedMotion', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('follows the reduce-motion setting of the system', () => {
    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: query === '(prefers-reduced-motion: reduce)',
    }));

    expect(prefersReducedMotion()).toBe(true);
  });

  it('assumes motion is fine where the browser cannot tell', () => {
    vi.stubGlobal('matchMedia', undefined);

    expect(prefersReducedMotion()).toBe(false);
  });
});

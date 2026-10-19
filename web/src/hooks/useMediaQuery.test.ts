// @vitest-environment jsdom
import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useMediaQuery } from './useMediaQuery';

/** A media query list whose match the test sets, telling its listeners as a browser would. */
class FakeMediaQueryList {
  private readonly listeners = new Set<() => void>();
  constructor(public matches: boolean) {}
  addEventListener(_type: 'change', listener: () => void) {
    this.listeners.add(listener);
  }
  removeEventListener(_type: 'change', listener: () => void) {
    this.listeners.delete(listener);
  }
  change(matches: boolean) {
    this.matches = matches;
    this.listeners.forEach((listener) => listener());
  }
}

describe('useMediaQuery', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('follows the query as the viewport changes', () => {
    const list = new FakeMediaQueryList(false);
    vi.stubGlobal('matchMedia', () => list);
    const { result } = renderHook(() => useMediaQuery('(max-width: 1023px)'));

    expect(result.current).toBe(false);
    act(() => list.change(true));
    expect(result.current).toBe(true);
  });

  it('matches nothing where the browser cannot tell', () => {
    vi.stubGlobal('matchMedia', undefined);

    expect(renderHook(() => useMediaQuery('(max-width: 1023px)')).result.current).toBe(false);
  });
});

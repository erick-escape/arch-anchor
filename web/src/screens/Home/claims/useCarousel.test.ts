// @vitest-environment jsdom
import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useCarousel } from './useCarousel';

describe('useCarousel', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('advances on its own and wraps after the last slide', () => {
    const { result } = renderHook(() => useCarousel(3, { intervalMs: 1000, still: false }));

    act(() => vi.advanceTimersByTime(1000));
    expect(result.current.index).toBe(1);
    act(() => vi.advanceTimersByTime(1000));
    act(() => vi.advanceTimersByTime(1000));
    expect(result.current.index).toBe(0);
  });

  it('stays put while paused, as when the pointer or focus is on it', () => {
    const { result } = renderHook(() => useCarousel(3, { intervalMs: 1000, still: false }));

    act(() => result.current.pause());
    act(() => vi.advanceTimersByTime(5000));
    expect(result.current.index).toBe(0);
    act(() => result.current.resume());
    act(() => vi.advanceTimersByTime(1000));
    expect(result.current.index).toBe(1);
  });

  it('never moves on its own under reduced motion, but still follows the controls', () => {
    const { result } = renderHook(() => useCarousel(3, { intervalMs: 1000, still: true }));

    act(() => vi.advanceTimersByTime(5000));
    expect(result.current.index).toBe(0);
    act(() => result.current.previous());
    expect(result.current.index).toBe(2);
    act(() => result.current.pick(1));
    expect(result.current.index).toBe(1);
    act(() => result.current.next());
    expect(result.current.index).toBe(2);
  });

  it('restarts the wait after a manual change, so a slide just chosen is not skipped at once', () => {
    const { result } = renderHook(() => useCarousel(3, { intervalMs: 1000, still: false }));

    act(() => vi.advanceTimersByTime(900));
    act(() => result.current.pick(2));
    act(() => vi.advanceTimersByTime(900));
    expect(result.current.index).toBe(2);
  });
});

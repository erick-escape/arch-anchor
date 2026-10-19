// @vitest-environment jsdom
import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { usePanelLayout } from './usePanelLayout';

function viewport(narrow: boolean) {
  vi.stubGlobal('matchMedia', () => ({
    matches: narrow,
    addEventListener: () => {},
    removeEventListener: () => {},
  }));
}

describe('usePanelLayout', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('opens both panels on a wide screen, and folds each on its own', () => {
    viewport(false);
    const { result } = renderHook(() => usePanelLayout());

    expect(result.current.open).toEqual({ modules: true, recommendations: true });
    act(() => result.current.toggle('modules'));
    expect(result.current.open).toEqual({ modules: false, recommendations: true });
  });

  it('keeps both closed on a narrow screen, where one drawer at a time covers the graph', () => {
    viewport(true);
    const { result } = renderHook(() => usePanelLayout());

    expect(result.current.open).toEqual({ modules: false, recommendations: false });
    act(() => result.current.show('modules'));
    act(() => result.current.show('recommendations'));
    expect(result.current.open).toEqual({ modules: false, recommendations: true });
  });
});

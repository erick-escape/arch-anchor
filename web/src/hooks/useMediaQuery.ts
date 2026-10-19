import { useCallback, useSyncExternalStore } from 'react';

/**
 * Whether a media query matches, kept up to date as the viewport changes. False where the
 * browser has no matchMedia (tests that do not stub it).
 *
 * @example const narrow = useMediaQuery('(max-width: 1023px)');
 */
export function useMediaQuery(query: string): boolean {
  const subscribe = useCallback(
    (onChange: () => void) => {
      const list = window.matchMedia?.(query);
      list?.addEventListener('change', onChange);
      return () => list?.removeEventListener('change', onChange);
    },
    [query]
  );
  return useSyncExternalStore(
    subscribe,
    () => window.matchMedia?.(query).matches ?? false,
    () => false
  );
}

import { useCallback, useEffect, useState, useSyncExternalStore } from 'react';
import type { ColorSchemeQuery } from './ColorSchemeQuery';
import type { ThemeMode } from './palette';
import type { ThemePreferenceStore } from './ThemePreferenceStore';
import { resolveThemeMode, type ThemePreference } from './themePreference';

export interface ThemeState {
  preference: ThemePreference;
  mode: ThemeMode;
  setPreference: (preference: ThemePreference) => void;
}

/**
 * The viewer's theme choice and the theme it resolves to, kept in `store` and following the OS
 * through `colorScheme` while the choice is "system". Marks <html data-theme> so the CSS
 * variables of cssVariables.ts apply.
 */
export function useThemePreference(
  store: ThemePreferenceStore,
  colorScheme: ColorSchemeQuery
): ThemeState {
  const [preference, setStoredPreference] = useState<ThemePreference>(() => store.read());
  const prefersDark = useSyncExternalStore(
    (onChange) => colorScheme.subscribe(onChange),
    () => colorScheme.prefersDark()
  );
  const mode = resolveThemeMode(preference, prefersDark);
  useEffect(() => {
    document.documentElement.dataset.theme = mode;
  }, [mode]);
  const setPreference = useCallback(
    (next: ThemePreference) => {
      store.write(next);
      setStoredPreference(next);
    },
    [store]
  );
  return { preference, mode, setPreference };
}

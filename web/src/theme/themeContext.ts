import { createContext, useContext } from 'react';
import type { ThemeState } from './useThemePreference';

export const ThemeContext = createContext<ThemeState | null>(null);

/**
 * The current theme and the setter of the viewer's choice. Throws outside a ThemeProvider,
 * which every screen sits in.
 *
 * @example const { mode, setPreference } = useTheme();
 */
export function useTheme(): ThemeState {
  const state = useContext(ThemeContext);
  if (!state) throw new Error('useTheme() needs a <ThemeProvider> above it; found none');
  return state;
}

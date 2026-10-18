import type { ThemeMode } from './palette';

export const THEME_PREFERENCES = ['system', 'light', 'dark'] as const;
export type ThemePreference = (typeof THEME_PREFERENCES)[number];

/** The localStorage key; index.html reads the same key before first paint. */
export const THEME_PREFERENCE_KEY = 'archanchor.theme';

export function isThemePreference(value: unknown): value is ThemePreference {
  return THEME_PREFERENCES.some((preference) => preference === value);
}

/**
 * The theme a preference means right now. "system" follows the OS, the default.
 *
 * @example resolveThemeMode('system', true) // 'dark'
 */
export function resolveThemeMode(
  preference: ThemePreference,
  systemPrefersDark: boolean
): ThemeMode {
  if (preference === 'system') return systemPrefersDark ? 'dark' : 'light';
  return preference;
}

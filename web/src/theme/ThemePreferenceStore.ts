import { isThemePreference, THEME_PREFERENCE_KEY, type ThemePreference } from './themePreference';

/** Where the viewer's theme choice is kept. Injected into ThemeProvider so tests use a fake. */
export interface ThemePreferenceStore {
  read(): ThemePreference;
  write(preference: ThemePreference): void;
}

/**
 * The choice in localStorage. "system" is stored as no entry, so the OS decides. Storage can be
 * missing or throw (private windows, blocked site data); the app then follows the OS.
 */
export class LocalStorageThemePreferenceStore implements ThemePreferenceStore {
  constructor(private readonly storage: Storage | undefined = globalThis.localStorage) {}

  read(): ThemePreference {
    try {
      const stored = this.storage?.getItem(THEME_PREFERENCE_KEY);
      return isThemePreference(stored) ? stored : 'system';
    } catch {
      return 'system';
    }
  }

  write(preference: ThemePreference): void {
    try {
      if (preference === 'system') this.storage?.removeItem(THEME_PREFERENCE_KEY);
      else this.storage?.setItem(THEME_PREFERENCE_KEY, preference);
    } catch {
      // Not remembered across visits; the choice still applies to this page.
    }
  }
}

import type { ThemePreferenceStore } from '../../theme/ThemePreferenceStore';
import type { ThemePreference } from '../../theme/themePreference';

/** Keeps the theme choice in memory and records every write. */
export class FakeThemePreferenceStore implements ThemePreferenceStore {
  readonly writes: ThemePreference[] = [];

  constructor(private stored: ThemePreference = 'system') {}

  read(): ThemePreference {
    return this.stored;
  }

  write(preference: ThemePreference): void {
    this.stored = preference;
    this.writes.push(preference);
  }
}

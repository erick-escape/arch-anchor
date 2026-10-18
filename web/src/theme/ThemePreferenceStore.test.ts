// @vitest-environment jsdom
import { beforeEach, describe, expect, it } from 'vitest';
import { LocalStorageThemePreferenceStore } from './ThemePreferenceStore';
import { THEME_PREFERENCE_KEY } from './themePreference';

class ThrowingStorage implements Pick<Storage, 'getItem' | 'setItem' | 'removeItem'> {
  getItem(): string | null {
    throw new Error('blocked');
  }
  setItem(): void {
    throw new Error('blocked');
  }
  removeItem(): void {
    throw new Error('blocked');
  }
}

describe('LocalStorageThemePreferenceStore', () => {
  beforeEach(() => localStorage.clear());

  it('follows the system until a choice is made', () => {
    expect(new LocalStorageThemePreferenceStore(localStorage).read()).toBe('system');
  });

  it('remembers an explicit choice and forgets it when the system is chosen again', () => {
    const store = new LocalStorageThemePreferenceStore(localStorage);
    store.write('dark');
    expect(localStorage.getItem(THEME_PREFERENCE_KEY)).toBe('dark');
    store.write('system');
    expect(localStorage.getItem(THEME_PREFERENCE_KEY)).toBeNull();
  });

  it('ignores a stored value that is not a preference', () => {
    localStorage.setItem(THEME_PREFERENCE_KEY, 'sepia');
    expect(new LocalStorageThemePreferenceStore(localStorage).read()).toBe('system');
  });

  it('falls back to the system when storage throws', () => {
    const store = new LocalStorageThemePreferenceStore(new ThrowingStorage() as unknown as Storage);
    expect(store.read()).toBe('system');
    expect(() => store.write('dark')).not.toThrow();
  });
});

import { describe, expect, it } from 'vitest';
import { isThemePreference, resolveThemeMode } from './themePreference';

describe('resolveThemeMode', () => {
  it('follows the system when the preference is system', () => {
    expect(resolveThemeMode('system', true)).toBe('dark');
    expect(resolveThemeMode('system', false)).toBe('light');
  });

  it('keeps an explicit choice whatever the system prefers', () => {
    expect(resolveThemeMode('light', true)).toBe('light');
    expect(resolveThemeMode('dark', false)).toBe('dark');
  });
});

describe('isThemePreference', () => {
  it('accepts the three preferences and nothing else', () => {
    expect(['system', 'light', 'dark'].every(isThemePreference)).toBe(true);
    expect(isThemePreference('sepia')).toBe(false);
    expect(isThemePreference(null)).toBe(false);
  });
});

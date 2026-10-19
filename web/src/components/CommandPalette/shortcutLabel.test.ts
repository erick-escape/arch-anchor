import { describe, expect, it } from 'vitest';
import { isPaletteShortcut, shortcutLabelFor } from './shortcutLabel';

describe('shortcutLabelFor', () => {
  it('shows the Command key on a Mac and Ctrl elsewhere', () => {
    expect(shortcutLabelFor('MacIntel')).toBe('⌘K');
    expect(shortcutLabelFor('Linux x86_64')).toBe('Ctrl K');
    expect(shortcutLabelFor('Win32')).toBe('Ctrl K');
  });
});

describe('isPaletteShortcut', () => {
  it('takes ⌘K and Ctrl+K, not a plain K', () => {
    expect(isPaletteShortcut({ key: 'k', metaKey: true, ctrlKey: false })).toBe(true);
    expect(isPaletteShortcut({ key: 'K', metaKey: false, ctrlKey: true })).toBe(true);
    expect(isPaletteShortcut({ key: 'k', metaKey: false, ctrlKey: false })).toBe(false);
  });
});

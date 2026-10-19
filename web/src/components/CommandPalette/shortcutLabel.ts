/**
 * How the palette's shortcut is written on this platform: ⌘K on a Mac, Ctrl K elsewhere.
 *
 * @example shortcutLabelFor(navigator.platform); // 'Ctrl K' on Linux
 */
export function shortcutLabelFor(platform: string): string {
  return /mac|iphone|ipad/i.test(platform) ? '⌘K' : 'Ctrl K';
}

/**
 * Whether a key press opens the palette: ⌘K or Ctrl+K, on any platform, so a Mac user on a
 * PC keyboard is not caught out.
 *
 * @example window.addEventListener('keydown', (event) => isPaletteShortcut(event) && open());
 */
export function isPaletteShortcut(
  event: Pick<KeyboardEvent, 'key' | 'metaKey' | 'ctrlKey'>
): boolean {
  return event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey);
}

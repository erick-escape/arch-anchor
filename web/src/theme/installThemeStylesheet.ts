import { themeStylesheet } from './cssVariables';

const STYLE_ID = 'aa-theme';

/**
 * Writes the token variables into <head> before React renders, so the first paint already has
 * the theme index.html picked. Idempotent.
 *
 * @example installThemeStylesheet(document)
 */
export function installThemeStylesheet(target: Document): void {
  if (target.getElementById(STYLE_ID)) return;
  const style = target.createElement('style');
  style.id = STYLE_ID;
  style.textContent = themeStylesheet();
  target.head.appendChild(style);
}

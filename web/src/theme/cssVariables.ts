import {
  COLOR_TOKENS,
  PALETTES,
  SCALES,
  SHADOW_TOKENS,
  SHADOWS,
  THEME_MODES,
  type ThemeMode,
} from './palette';

const PREFIX = '--aa-';

/**
 * A token as a CSS value, for the few places that style through JS (React Flow props, SVG).
 *
 * @example cssVariable('accent') // 'var(--aa-accent)'
 */
export function cssVariable(token: string): string {
  return `var(${PREFIX}${token})`;
}

function declarationsOf(values: Record<string, string>): string {
  return Object.entries(values)
    .map(([token, value]) => `  ${PREFIX}${token}: ${value};`)
    .join('\n');
}

function themeBlockOf(mode: ThemeMode): string {
  const colours: Record<string, string> = {};
  for (const token of COLOR_TOKENS) colours[token] = PALETTES[mode][token];
  for (const token of SHADOW_TOKENS) colours[token] = SHADOWS[mode][token];
  const selector =
    mode === 'light' ? ':root, :root[data-theme="light"]' : ':root[data-theme="dark"]';
  return `${selector} {\n${declarationsOf(colours)}\n  color-scheme: ${mode};\n}`;
}

/**
 * Every token as a CSS custom property: the light palette on :root, each theme under its
 * data-theme attribute (set by ThemeProvider, and before first paint by index.html), and the
 * shared scales once.
 */
export function themeStylesheet(): string {
  const themes = THEME_MODES.map(themeBlockOf).join('\n');
  return `${themes}\n:root {\n${declarationsOf(SCALES)}\n}\n`;
}

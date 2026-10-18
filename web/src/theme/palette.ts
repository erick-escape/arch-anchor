/**
 * The one source of the app's colours, shadows and scales. The values are the ArchAnchor design
 * system's tokens (docs/design/redesign-brief.md, section 3), themselves derived from the palette
 * sampled from the logo in deck-kit/appsscript/00_theme.gs. cssVariables.ts turns them into CSS
 * custom properties and antdTheme.ts into antd's theme, so nothing else hard-codes a colour.
 */

export const THEME_MODES = ['light', 'dark'] as const;
export type ThemeMode = (typeof THEME_MODES)[number];

export const COLOR_TOKENS = [
  'page',
  'surface',
  'sunken',
  'raised',
  'accent-soft',
  'text',
  'text-strong',
  'text-muted',
  'link',
  'primary',
  'on-primary',
  'accent',
  'on-accent',
  'line',
  'line-strong',
  'line-brand',
  'conform',
  'conform-text',
  'divergence',
  'divergence-text',
  'absence',
  'absence-text',
  'mark-chevron',
  'mark-mountain',
  'mark-base',
  'wordmark-arch',
  'wordmark-anchor',
  'grid-dot',
] as const;
export type ColorToken = (typeof COLOR_TOKENS)[number];

export const PALETTES: Record<ThemeMode, Record<ColorToken, string>> = {
  light: {
    page: '#F7FAFE',
    surface: '#FFFFFF',
    sunken: '#EAF3FE',
    raised: '#FFFFFF',
    'accent-soft': '#E2EEFD',
    text: '#22282E',
    'text-strong': '#112749',
    'text-muted': '#677077',
    link: '#0E5FB6',
    primary: '#0E5FB6',
    'on-primary': '#FFFFFF',
    accent: '#2287F0',
    'on-accent': '#FFFFFF',
    line: '#D7DEE7',
    'line-strong': '#818C96',
    'line-brand': '#9DCAEE',
    conform: '#2E9E5B',
    'conform-text': '#257E49',
    divergence: '#D64545',
    'divergence-text': '#C53F3F',
    absence: '#B87F2F',
    'absence-text': '#936525',
    'mark-chevron': '#112749',
    'mark-mountain': '#0E5FB6',
    'mark-base': '#9DCAEE',
    'wordmark-arch': '#112749',
    'wordmark-anchor': '#2287F0',
    'grid-dot': '#C4DDF3',
  },
  dark: {
    page: '#0A1628',
    surface: '#112749',
    sunken: '#0E2142',
    raised: '#17325C',
    'accent-soft': '#15305A',
    text: '#EAF3FE',
    'text-strong': '#FFFFFF',
    'text-muted': '#9AA7B2',
    link: '#4C9EF3',
    primary: '#2287F0',
    'on-primary': '#0A1628',
    accent: '#2287F0',
    'on-accent': '#FFFFFF',
    line: '#22406E',
    'line-strong': '#677DA0',
    'line-brand': '#0E5FB6',
    conform: '#2E9E5B',
    'conform-text': '#4BAC72',
    divergence: '#D74B4B',
    'divergence-text': '#E27D7D',
    absence: '#E9A13B',
    'absence-text': '#E9A13B',
    'mark-chevron': '#EAF3FE',
    'mark-mountain': '#2287F0',
    'mark-base': '#9DCAEE',
    'wordmark-arch': '#EAF3FE',
    'wordmark-anchor': '#2287F0',
    'grid-dot': '#1C3A66',
  },
};

export const SHADOW_TOKENS = ['shadow-sm', 'shadow-lg', 'focus-ring', 'glow', 'scrim'] as const;
export type ShadowToken = (typeof SHADOW_TOKENS)[number];

export const SHADOWS: Record<ThemeMode, Record<ShadowToken, string>> = {
  light: {
    'shadow-sm': '0 1px 2px #1127490f, 0 1px 3px #1127491a',
    'shadow-lg': '0 4px 8px #11274914, 0 12px 28px -4px #11274929',
    'focus-ring': '0 0 0 2px #f7fafe, 0 0 0 4px #2287f0',
    glow: '0 0 0 4px #2287f033',
    scrim: '#11274966',
  },
  dark: {
    'shadow-sm': '0 1px 2px #00000066',
    'shadow-lg': '0 12px 28px -4px #000000b3, 0 0 0 1px #22406e',
    'focus-ring': '0 0 0 2px #0a1628, 0 0 0 4px #2287f0',
    glow: '0 0 0 1px #2287f0, 0 0 24px #2287f066',
    scrim: '#000000a6',
  },
};

/** Theme-independent scales: spacing on a 4 px base, radii, and the three font stacks. */
export const SCALES: Record<string, string> = {
  'space-1': '4px',
  'space-2': '8px',
  'space-3': '12px',
  'space-4': '16px',
  'space-5': '20px',
  'space-6': '24px',
  'space-8': '32px',
  'space-12': '48px',
  'space-16': '64px',
  'radius-sm': '4px',
  'radius-md': '8px',
  'radius-lg': '14px',
  'radius-xl': '20px',
  'radius-full': '9999px',
  'font-display': '"Jost Variable", Jost, "Century Gothic", "Avenir Next", system-ui, sans-serif',
  'font-ui': '"Inter Variable", Inter, system-ui, -apple-system, "Segoe UI", sans-serif',
  'font-mono': '"Roboto Mono", ui-monospace, Menlo, Consolas, monospace',
};

import { describe, expect, it } from 'vitest';
import { contrastRatio } from './contrast';
import { PALETTES, THEME_MODES, type ColorToken, type ThemeMode } from './palette';

const GROUNDS: Record<ThemeMode, ColorToken[]> = {
  light: ['page', 'surface', 'sunken'],
  dark: ['page', 'surface', 'sunken', 'raised'],
};
const TEXT: ColorToken[] = [
  'text',
  'text-strong',
  'text-muted',
  'link',
  'conform-text',
  'divergence-text',
  'absence-text',
];
const GRAPHICS: ColorToken[] = ['accent', 'line-strong', 'conform', 'divergence', 'absence'];

// The worst ratio of `token` over every ground of `mode`.
function worstRatio(mode: ThemeMode, token: ColorToken): number {
  const palette = PALETTES[mode];
  return Math.min(...GROUNDS[mode].map((ground) => contrastRatio(palette[token], palette[ground])));
}

// WCAG 2.x: text needs 4.5:1, graphics and control borders 3:1, on every ground they sit on.
describe.each(THEME_MODES)('the %s palette', (mode) => {
  it.each(TEXT)('keeps %s at 4.5:1 or more on every ground', (token) => {
    expect(worstRatio(mode, token)).toBeGreaterThanOrEqual(4.5);
  });

  it.each(GRAPHICS)('keeps %s at 3:1 or more on every ground', (token) => {
    expect(worstRatio(mode, token)).toBeGreaterThanOrEqual(3);
  });

  it('keeps the primary button label at 4.5:1 on its fill', () => {
    const palette = PALETTES[mode];
    expect(contrastRatio(palette['on-primary'], palette.primary)).toBeGreaterThanOrEqual(4.5);
  });

  it('keeps text and links readable on a selected row', () => {
    const palette = PALETTES[mode];
    expect(contrastRatio(palette.text, palette['accent-soft'])).toBeGreaterThanOrEqual(4.5);
    expect(contrastRatio(palette.link, palette['accent-soft'])).toBeGreaterThanOrEqual(4.5);
  });
});

// A dark mark on the dark page would vanish (1.22:1), which is why the chevron turns wash.
it('keeps the chevron of the mark visible on the page in both themes', () => {
  for (const mode of THEME_MODES) {
    const palette = PALETTES[mode];
    expect(contrastRatio(palette['mark-chevron'], palette.page)).toBeGreaterThanOrEqual(3);
  }
});

import { describe, expect, it } from 'vitest';
import { cssVariable, themeStylesheet } from './cssVariables';

describe('cssVariable', () => {
  it('names a token with the app prefix', () => {
    expect(cssVariable('text-strong')).toBe('var(--aa-text-strong)');
  });
});

describe('themeStylesheet', () => {
  const css = themeStylesheet();

  it('declares the light palette on the root and under data-theme="light"', () => {
    expect(css).toContain(':root, :root[data-theme="light"] {');
    expect(css).toMatch(/:root\[data-theme="light"\] \{[^}]*--aa-page: #F7FAFE;/);
  });

  it('redeclares the palette under data-theme="dark"', () => {
    expect(css).toMatch(/\[data-theme="dark"\] \{[^}]*--aa-page: #0A1628;[^}]*color-scheme: dark;/);
  });

  it('declares shadows per theme and the shared scales once', () => {
    expect(css).toMatch(/\[data-theme="dark"\] \{[^}]*--aa-focus-ring: [^;]*#2287f0;/);
    expect(css).toContain('--aa-radius-lg: 14px;');
    expect(css).toContain('--aa-font-display: "Jost Variable"');
  });
});

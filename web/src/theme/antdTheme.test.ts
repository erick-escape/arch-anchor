import { theme } from 'antd';
import { describe, expect, it } from 'vitest';
import { antdThemeOf } from './antdTheme';
import { PALETTES } from './palette';

describe('antdThemeOf', () => {
  it('takes the primary, text and surface colours from the palette', () => {
    const config = antdThemeOf('light');

    expect(config.token?.colorPrimary).toBe(PALETTES.light.primary);
    expect(config.token?.colorText).toBe(PALETTES.light.text);
    expect(config.token?.colorBgContainer).toBe(PALETTES.light.surface);
  });

  it('switches to the dark algorithm and the dark palette', () => {
    const config = antdThemeOf('dark');

    expect(config.algorithm).toBe(theme.darkAlgorithm);
    expect(config.token?.colorBgElevated).toBe(PALETTES.dark.raised);
  });
});

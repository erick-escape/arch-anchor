import { theme, type ThemeConfig } from 'antd';
import { PALETTES, SCALES, type ThemeMode } from './palette';

/**
 * antd's theme for a mode, taken from the same palette as the CSS variables so antd's modals,
 * inputs and tabs match the app's own components.
 *
 * @example <ConfigProvider theme={antdThemeOf('dark')}>…</ConfigProvider>
 */
export function antdThemeOf(mode: ThemeMode): ThemeConfig {
  const palette = PALETTES[mode];
  return {
    algorithm: mode === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm,
    token: {
      colorPrimary: palette.primary,
      colorLink: palette.link,
      colorText: palette.text,
      colorTextHeading: palette['text-strong'],
      colorTextSecondary: palette['text-muted'],
      colorTextDescription: palette['text-muted'],
      colorBgLayout: palette.page,
      colorBgContainer: palette.surface,
      colorBgElevated: palette.raised,
      colorBorder: palette['line-strong'],
      colorBorderSecondary: palette.line,
      colorError: palette.divergence,
      colorErrorText: palette['divergence-text'],
      colorSuccess: palette.conform,
      fontFamily: SCALES['font-ui'],
      fontFamilyCode: SCALES['font-mono'],
      borderRadius: 8,
      borderRadiusLG: 14,
    },
  };
}

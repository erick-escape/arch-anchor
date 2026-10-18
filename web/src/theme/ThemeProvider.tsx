import { App as AntdApp, ConfigProvider } from 'antd';
import { useMemo, type ReactNode } from 'react';
import { antdThemeOf } from './antdTheme';
import type { ColorSchemeQuery } from './ColorSchemeQuery';
import type { ThemePreferenceStore } from './ThemePreferenceStore';
import { ThemeContext } from './themeContext';
import { useThemePreference } from './useThemePreference';

interface ThemeProviderProps {
  store: ThemePreferenceStore;
  colorScheme: ColorSchemeQuery;
  children: ReactNode;
}

/**
 * Light or dark for the whole app: the CSS variables, antd's theme and antd's App context (for
 * message and modal), from the one preference.
 *
 * @example
 * <ThemeProvider store={new LocalStorageThemePreferenceStore()} colorScheme={new MatchMediaColorSchemeQuery()}>
 *   <App />
 * </ThemeProvider>
 */
export function ThemeProvider({ store, colorScheme, children }: ThemeProviderProps) {
  const state = useThemePreference(store, colorScheme);
  const antdTheme = useMemo(() => antdThemeOf(state.mode), [state.mode]);
  return (
    <ThemeContext.Provider value={state}>
      <ConfigProvider theme={antdTheme}>
        <AntdApp>{children}</AntdApp>
      </ConfigProvider>
    </ThemeContext.Provider>
  );
}

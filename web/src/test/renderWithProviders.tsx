import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderResult } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ThemeProvider } from '../theme/ThemeProvider';
import type { ThemePreference } from '../theme/themePreference';
import { FakeColorSchemeQuery } from './fakes/FakeColorSchemeQuery';
import { FakeThemePreferenceStore } from './fakes/FakeThemePreferenceStore';

export interface ProviderOptions {
  /** The URL the screen opens at. */
  route?: string;
  /** The route pattern the element is mounted under, e.g. '/analyze/:projectName'. */
  path?: string;
  themePreference?: ThemePreference;
  systemPrefersDark?: boolean;
}

export interface RenderedWithProviders extends RenderResult {
  themeStore: FakeThemePreferenceStore;
  colorScheme: FakeColorSchemeQuery;
  queryClient: QueryClient;
}

/**
 * Renders a screen or component the way main.tsx mounts the app: theme (with fake storage and a
 * fake OS colour scheme), antd's App context, a fresh query client and a router.
 *
 * @example renderWithProviders(<ProjectsPage />, { route: '/projects' })
 */
export function renderWithProviders(
  ui: ReactElement,
  options: ProviderOptions = {}
): RenderedWithProviders {
  const themeStore = new FakeThemePreferenceStore(options.themePreference);
  const colorScheme = new FakeColorSchemeQuery(options.systemPrefersDark);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const result = render(
    <ThemeProvider store={themeStore} colorScheme={colorScheme}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[options.route ?? '/']}>
          {routed(ui, options.path)}
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>
  );
  return { ...result, themeStore, colorScheme, queryClient };
}

function routed(ui: ReactElement, path: string | undefined): ReactElement {
  if (!path) return ui;
  return (
    <Routes>
      <Route path={path} element={ui} />
    </Routes>
  );
}

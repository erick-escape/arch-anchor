import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import '@fontsource-variable/inter';
import '@fontsource-variable/jost';
import '@fontsource/roboto-mono/400.css';
import '@fontsource/roboto-mono/500.css';
import './index.css';
import App from './App.tsx';
import { MatchMediaColorSchemeQuery } from './theme/ColorSchemeQuery';
import { installThemeStylesheet } from './theme/installThemeStylesheet';
import { ThemeProvider } from './theme/ThemeProvider';
import { LocalStorageThemePreferenceStore } from './theme/ThemePreferenceStore';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

installThemeStylesheet(document);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider
      store={new LocalStorageThemePreferenceStore()}
      colorScheme={new MatchMediaColorSchemeQuery()}
    >
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>
);

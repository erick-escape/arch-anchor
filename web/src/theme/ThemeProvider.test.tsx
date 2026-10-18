// @vitest-environment jsdom
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { FakeColorSchemeQuery } from '../test/fakes/FakeColorSchemeQuery';
import { FakeThemePreferenceStore } from '../test/fakes/FakeThemePreferenceStore';
import { useTheme } from './themeContext';
import { ThemeProvider } from './ThemeProvider';

function ThemeReadout() {
  const { mode, preference, setPreference } = useTheme();
  return (
    <button type="button" onClick={() => setPreference('dark')}>
      {preference} → {mode}
    </button>
  );
}

function renderWith(store: FakeThemePreferenceStore, query: FakeColorSchemeQuery) {
  return render(
    <ThemeProvider store={store} colorScheme={query}>
      <ThemeReadout />
    </ThemeProvider>
  );
}

describe('ThemeProvider', () => {
  it('follows the system by default and marks the document with the theme', () => {
    renderWith(new FakeThemePreferenceStore(), new FakeColorSchemeQuery(true));

    expect(screen.getByRole('button')).toHaveTextContent('system → dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('follows the system when it changes while the page is open', () => {
    const query = new FakeColorSchemeQuery(false);
    renderWith(new FakeThemePreferenceStore(), query);

    act(() => query.switchTo('dark'));

    expect(screen.getByRole('button')).toHaveTextContent('system → dark');
  });

  it('applies and remembers an explicit choice', async () => {
    const store = new FakeThemePreferenceStore();
    renderWith(store, new FakeColorSchemeQuery(false));

    await userEvent.click(screen.getByRole('button'));

    expect(screen.getByRole('button')).toHaveTextContent('dark → dark');
    expect(store.writes).toEqual(['dark']);
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('starts from a remembered choice', () => {
    renderWith(new FakeThemePreferenceStore('light'), new FakeColorSchemeQuery(true));

    expect(screen.getByRole('button')).toHaveTextContent('light → light');
  });
});

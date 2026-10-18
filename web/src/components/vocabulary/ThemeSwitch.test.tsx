// @vitest-environment jsdom
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '../../test/renderWithProviders';
import { ThemeSwitch } from './ThemeSwitch';

describe('ThemeSwitch', () => {
  it('starts on System and shows which choice is pressed', () => {
    renderWithProviders(<ThemeSwitch />);

    expect(screen.getByRole('group', { name: 'Theme' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'System' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Dark' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('switches the document to the theme picked and remembers it', async () => {
    const { themeStore } = renderWithProviders(<ThemeSwitch />);

    await userEvent.click(screen.getByRole('button', { name: 'Dark' }));

    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(themeStore.writes).toEqual(['dark']);
    expect(screen.getByRole('button', { name: 'Dark' })).toHaveAttribute('aria-pressed', 'true');
  });
});

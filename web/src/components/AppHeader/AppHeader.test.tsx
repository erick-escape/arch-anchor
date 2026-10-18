// @vitest-environment jsdom
import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '../../test/renderWithProviders';
import { AppHeader } from './AppHeader';

describe('AppHeader', () => {
  it('takes the logo home', () => {
    renderWithProviders(<AppHeader />);

    expect(screen.getByRole('link', { name: 'ArchAnchor home' })).toHaveAttribute('href', '/');
  });

  it('offers Projects, How it works, Research and GitHub', () => {
    renderWithProviders(<AppHeader />);

    const nav = within(screen.getByRole('navigation', { name: 'Main' }));
    expect(nav.getByRole('link', { name: 'Projects' })).toHaveAttribute('href', '/projects');
    expect(nav.getByRole('link', { name: 'How it works' })).toHaveAttribute('href', '/#how');
    expect(nav.getByRole('link', { name: 'Research' })).toHaveAttribute('href', '/#research');
    expect(nav.getByRole('link', { name: 'GitHub' })).toHaveAttribute(
      'href',
      'https://github.com/erick-escape/arch-anchor'
    );
  });

  it('marks the page the viewer is on', () => {
    renderWithProviders(<AppHeader />, { route: '/projects' });

    expect(screen.getByRole('link', { name: 'Projects' })).toHaveAttribute('aria-current', 'page');
  });

  it('carries the theme switch', () => {
    renderWithProviders(<AppHeader />);

    expect(screen.getByRole('group', { name: 'Theme' })).toBeInTheDocument();
  });
});

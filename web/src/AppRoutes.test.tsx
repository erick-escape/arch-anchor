// @vitest-environment jsdom
import { screen } from '@testing-library/react';
import axios, { type AxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { FakeAxiosAdapter } from './test/fakes/FakeAxiosAdapter';
import { renderWithProviders } from './test/renderWithProviders';

describe('AppRoutes', () => {
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = new FakeAxiosAdapter().respondTo('GET', '/api/projects', {
      projects: [],
    }).adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
  });

  it('opens the projects at /projects', async () => {
    renderWithProviders(<AppRoutes />, { route: '/projects' });

    expect(await screen.findByRole('heading', { name: 'Projects', level: 1 })).toBeInTheDocument();
  });

  it('says so when a page does not exist, with a way home', async () => {
    renderWithProviders(<AppRoutes />, { route: '/nowhere' });

    expect(
      await screen.findByRole('heading', { name: 'This page does not exist' })
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go to the homepage' })).toHaveAttribute('href', '/');
  });
});

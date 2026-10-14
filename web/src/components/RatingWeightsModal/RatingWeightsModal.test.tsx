// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import axios, { AxiosRequestConfig } from 'axios';
import { message } from 'antd';
import RatingWeightsModal from './index';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { RatingWeights } from '../../interface/RatingWeights';

const WEIGHTS_URL = '/api/projects/shop/rating-weights';

const EQUAL: RatingWeights = {
  election: { similarity: 0.5, violation: 0.5 },
  recommendation: { similarity: 0.5, violation: 0.5 },
};

describe('RatingWeightsModal', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter()
      .respondTo('GET', WEIGHTS_URL, EQUAL)
      .respondTo('PUT', WEIGHTS_URL, EQUAL);
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
    message.destroy();
  });

  function renderModal(onSaved = vi.fn(), onClose = vi.fn()) {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <RatingWeightsModal projectName="shop" open onClose={onClose} onSaved={onSaved} />
      </QueryClientProvider>
    );
  }

  function sentBodies(): unknown[] {
    return fakeApi
      .requestsTo('PUT', WEIGHTS_URL)
      .map((request) => JSON.parse(String(request.body)));
  }

  function electionSlider(): Promise<HTMLElement> {
    return screen.findByRole('slider', { name: 'Election: similarity weight' });
  }

  it("shows the project's weights for the election and the recommendation rate", async () => {
    fakeApi.respondTo('GET', WEIGHTS_URL, {
      ...EQUAL,
      election: { similarity: 0.8, violation: 0.2 },
    });
    renderModal();

    expect(await electionSlider()).toHaveAttribute('aria-valuenow', '0.8');
    expect(screen.getByText('Similarity 0.80 · Violations 0.20')).toBeInTheDocument();
    expect(
      screen.getByRole('slider', { name: 'Recommendation rate: similarity weight' })
    ).toHaveAttribute('aria-valuenow', '0.5');
    expect(screen.getByText('Similarity 0.50 · Violations 0.50')).toBeInTheDocument();
  });

  // Changing the election replaces reference classes, so the architect is told which ones.
  it('says that a new election keeps the reference classes chosen by hand', async () => {
    renderModal();

    await electionSlider();

    expect(
      screen.getByText(/except those whose reference classes you chose by hand/)
    ).toBeVisible();
  });

  it('saves the pair a slider was moved to, then reports and closes', async () => {
    const onSaved = vi.fn();
    const onClose = vi.fn();
    renderModal(onSaved, onClose);

    fireEvent.keyDown(await electionSlider(), { key: 'ArrowRight', keyCode: 39, which: 39 });
    expect(screen.getByText('Similarity 0.55 · Violations 0.45')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    await vi.waitFor(() => expect(onSaved).toHaveBeenCalledOnce());
    expect(onClose).toHaveBeenCalledOnce();
    expect(sentBodies()).toEqual([
      { election: { similarity: 0.55, violation: 0.45 }, recommendation: EQUAL.recommendation },
    ]);
  });

  it('shows why the API rejected the weights and stays open', async () => {
    const detail = 'Invalid election weights. Expected two weights in [0, 1] summing to 1';
    fakeApi.failWith('PUT', WEIGHTS_URL, 400, { status: 400, detail });
    const onSaved = vi.fn();
    const onClose = vi.fn();
    renderModal(onSaved, onClose);

    await electionSlider();
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText(detail)).toBeInTheDocument();
    expect(onSaved).not.toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  });
});

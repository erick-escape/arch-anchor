// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import axios, { AxiosRequestConfig } from 'axios';
import { message } from 'antd';
import SettingsModal from './index';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { RatingWeights } from '../../interface/RatingWeights';
import { AnalysisSettings } from '../../interface/AnalysisSettings';

const WEIGHTS_URL = '/api/projects/shop/rating-weights';

const SETTINGS_URL = '/api/projects/shop/analysis-settings';

const DEFAULT_SETTINGS: AnalysisSettings = {
  minSplitModuleClasses: 1,
  minSourceClassesAfterMove: 1,
  maxMoveTargetsPerClass: null,
  minClassDependencies: 0,
  minRecommendationRate: 0,
  minMergeSimilarityChange: null,
};

const EQUAL: RatingWeights = {
  election: { similarity: 0.5, violation: 0.5 },
  recommendation: { similarity: 0.5, violation: 0.5 },
};

describe('SettingsModal', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter()
      .respondTo('GET', WEIGHTS_URL, EQUAL)
      .respondTo('PUT', WEIGHTS_URL, EQUAL)
      .respondTo('GET', SETTINGS_URL, DEFAULT_SETTINGS)
      .respondTo('PUT', SETTINGS_URL, DEFAULT_SETTINGS);
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
        <SettingsModal projectName="shop" open onClose={onClose} onSaved={onSaved} />
      </QueryClientProvider>
    );
  }

  function sentBodies(url = WEIGHTS_URL): unknown[] {
    return fakeApi.requestsTo('PUT', url).map((request) => JSON.parse(String(request.body)));
  }

  async function openRecommendationsTab(): Promise<void> {
    await userEvent.click(await screen.findByRole('tab', { name: 'Recommendations' }));
  }

  function settingInput(label: string): Promise<HTMLElement> {
    return screen.findByRole('spinbutton', { name: label });
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

    // Not toBeVisible: jsdom sees the modal mid-animation, at opacity 0.
    expect(
      screen.getByText(/except those whose reference classes you chose by hand/)
    ).toBeInTheDocument();
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

  describe('recommendations tab (#36)', () => {
    it("shows the project's analysis settings", async () => {
      fakeApi.respondTo('GET', SETTINGS_URL, {
        minSplitModuleClasses: 2,
        minSourceClassesAfterMove: 3,
        maxMoveTargetsPerClass: 1,
        minClassDependencies: 5,
        minRecommendationRate: 0.2,
        minMergeSimilarityChange: -0.1,
      });
      renderModal();

      await openRecommendationsTab();

      expect(await settingInput('Fewest classes in each half of a split')).toHaveValue('2');
      expect(await settingInput('Fewest classes a move leaves in its module')).toHaveValue('3');
      expect(await settingInput('Move targets per class')).toHaveValue('1');
      for (const noLimit of screen.getAllByRole('checkbox', { name: 'No limit' })) {
        expect(noLimit).not.toBeChecked();
      }
      expect(await settingInput('Fewest package dependencies of a judged class')).toHaveValue('5');
      expect(await settingInput('Lowest rate of a shown split, merge or move')).toHaveValue('0.20');
      expect(await settingInput('Lowest similarity change of a recommended merge')).toHaveValue(
        '-0.10'
      );
    });

    it('says that violations are always shown', async () => {
      renderModal();

      await openRecommendationsTab();
      await settingInput('Lowest rate of a shown split, merge or move');

      expect(screen.getByText(/Violations have no rate and are always shown/)).toBeInTheDocument();
    });

    it('saves the changed settings, then reports and closes', async () => {
      const onSaved = vi.fn();
      const onClose = vi.fn();
      renderModal(onSaved, onClose);
      await openRecommendationsTab();

      const splitHalf = await settingInput('Fewest classes in each half of a split');
      await userEvent.clear(splitHalf);
      await userEvent.type(splitHalf, '2');
      const [noTargetLimit, noMergeLimit] = screen.getAllByRole('checkbox', { name: 'No limit' });
      await userEvent.click(noTargetLimit);
      await userEvent.click(noMergeLimit);
      await userEvent.click(screen.getByRole('button', { name: 'Save' }));

      await vi.waitFor(() => expect(onSaved).toHaveBeenCalledOnce());
      expect(onClose).toHaveBeenCalledOnce();
      expect(sentBodies(SETTINGS_URL)).toEqual([
        {
          ...DEFAULT_SETTINGS,
          minSplitModuleClasses: 2,
          maxMoveTargetsPerClass: 1,
          minMergeSimilarityChange: 0,
        },
      ]);
    });

    it('shows why the API rejected the settings and stays open', async () => {
      const detail = 'Expected minSplitModuleClasses to be an integer >= 1, got 0';
      fakeApi.failWith('PUT', SETTINGS_URL, 400, { status: 400, detail });
      const onSaved = vi.fn();
      const onClose = vi.fn();
      renderModal(onSaved, onClose);
      await openRecommendationsTab();

      await settingInput('Fewest classes in each half of a split');
      await userEvent.click(screen.getByRole('button', { name: 'Save' }));

      expect(await screen.findByText(detail)).toBeInTheDocument();
      expect(onSaved).not.toHaveBeenCalled();
      expect(onClose).not.toHaveBeenCalled();
    });
  });
});

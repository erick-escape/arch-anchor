// @vitest-environment jsdom
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { AxiosRequestConfig } from 'axios';
import AnalyzePageWithProvider from './index';
import { api } from '../../services/api';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';
import { installReactFlowJsdomShims } from '../../test/reactFlowJsdom';
import { renderWithProviders } from '../../test/renderWithProviders';
import { clazzFixture, moduleFixture } from '../../test/fixtures/moduleFixtures';
import {
  moveRecommendationFixture,
  recommendationsFixture,
  violationRecommendationFixture,
} from '../../test/fixtures/recommendationFixtures';
import { CommandPaletteProvider } from '../../components/CommandPalette/CommandPaletteProvider';
import { ProjectAnalyses } from '../../interface/ProjectAnalyses';

const analyses: ProjectAnalyses = {
  id: 'p1',
  projectName: 'shop',
  modulesList: [
    moduleFixture({ id: 'm1', name: 'orders' }),
    moduleFixture({ id: 'm2', name: 'billing' }),
  ],
  projectSimilarity: 0.6,
  architecturalConstraints: [],
};

const noRecommendations = {
  splits: [],
  merges: [],
  moves: [],
  violations: [],
  summary: { totalRecommendations: 0, highPriority: 0, mediumPriority: 0, lowPriority: 0 },
};

describe('AnalyzePage', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapters: AxiosRequestConfig['adapter'][];

  beforeAll(() => installReactFlowJsdomShims());

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter()
      .respondTo('POST', '/api/analyze', analyses)
      .respondTo('GET', '/api/recommendations', noRecommendations);
    // The recommendations client is its own axios instance, so it needs the fake too.
    realAdapters = [axios.defaults.adapter, api.defaults.adapter];
    axios.defaults.adapter = fakeApi.adapter;
    api.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    [axios.defaults.adapter, api.defaults.adapter] = realAdapters;
  });

  function renderPage() {
    return renderWithProviders(<AnalyzePageWithProvider />, {
      route: '/analyze/shop',
      path: '/analyze/:projectName',
    });
  }

  // The module of `name` on the graph.
  function moduleNode(name: string): Promise<HTMLElement> {
    return screen.findByRole('region', { name: `Module ${name}` });
  }

  it('analyzes the project named in the URL and states what it found', async () => {
    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'shop · 2 modules · no violations' })
    ).toBeInTheDocument();
    expect(fakeApi.requestsTo('POST', '/api/analyze')[0].params).toEqual({ projectName: 'shop' });
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveTextContent(
      'Projects/shop'
    );
  });

  it('draws a node and lists a sidebar card for every module', async () => {
    renderPage();

    await moduleNode('orders');

    expect(screen.getAllByText('orders')).toHaveLength(2);
    expect(screen.getAllByText('billing')).toHaveLength(2);
  });

  // The API answers GET /api/recommendations for the project analysed last, so asking before
  // this project's analysis returns showed the previous project's cards first (#53).
  it('asks for the recommendations only once the analysis has returned', async () => {
    renderPage();

    await moduleNode('orders');

    const urls = fakeApi.requests.map((request) => `${request.method} ${request.url}`);
    expect(urls.indexOf('GET /api/recommendations')).toBeGreaterThan(
      urls.indexOf('POST /api/analyze')
    );
  });

  it('says why when the analysis fails, and analyzes again on request', async () => {
    fakeApi.failWith('POST', '/api/analyze', 500, { detail: 'The project has no Java sources' });
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('The project has no Java sources');
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));

    await vi.waitFor(() => expect(fakeApi.requestsTo('POST', '/api/analyze')).toHaveLength(2));
  });

  it('previews the constraints of the modules on screen before exporting them', async () => {
    renderPage();
    await moduleNode('orders');

    await userEvent.click(screen.getByRole('button', { name: 'Export ACs' }));

    const dialog = within(await screen.findByRole('dialog'));
    expect(dialog.getByRole('region', { name: 'orders' })).toBeInTheDocument();
    expect(dialog.getByRole('region', { name: 'billing' })).toBeInTheDocument();
    expect(dialog.getByRole('button', { name: 'Download PDF' })).toBeInTheDocument();
  });

  // Re-analysing reads the sources again and discards the module operation (#19).
  it('shows the saved modules after a recommendation is applied, without analyzing again', async () => {
    fakeApi
      .respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ moves: [moveRecommendationFixture()] })
      )
      .respondTo('POST', '/api/recommendations/apply/move', {})
      .respondTo('GET', '/api/module', [
        moduleFixture({ id: 'm1', name: 'orders', similarity: 1 }),
        moduleFixture({ id: 'm2', name: 'billing' }),
      ]);
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Apply move…' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Apply move' }));

    await vi.waitFor(async () =>
      expect(within(await moduleNode('orders')).getByText('100.00%')).toBeInTheDocument()
    );
    expect(fakeApi.requestsTo('POST', '/api/analyze')).toHaveLength(1);
  });

  // New weights re-elect the saved modules and re-rate the recommendations on the server
  // (#27); the page shows both without analyzing again, which would discard module operations.
  it('shows the re-elected modules and new recommendations after the weights change', async () => {
    const repository = clazzFixture({ id: 'c2', name: 'OrderRepository' });
    const equalWeights = {
      election: { similarity: 0.5, violation: 0.5 },
      recommendation: { similarity: 0.5, violation: 0.5 },
    };
    fakeApi
      .respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ moves: [moveRecommendationFixture()] })
      )
      .respondTo('GET', '/api/projects/shop/rating-weights', equalWeights)
      .respondTo('PUT', '/api/projects/shop/rating-weights', equalWeights)
      .respondTo('GET', '/api/module', [
        moduleFixture({ id: 'm1', name: 'orders', refClazzes: [repository] }),
        moduleFixture({ id: 'm2', name: 'billing' }),
      ]);
    renderPage();
    await screen.findByRole('button', { name: 'Apply move…' });
    fakeApi.respondTo('GET', '/api/recommendations', noRecommendations);

    await userEvent.click(screen.getByRole('button', { name: 'Settings' }));
    fireEvent.keyDown(await screen.findByRole('slider', { name: 'Election: similarity weight' }), {
      key: 'ArrowRight',
      keyCode: 39,
      which: 39,
    });
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(
      await within(await moduleNode('orders')).findByText('OrderRepository')
    ).toBeInTheDocument();
    await vi.waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Apply move…' })).toBeNull()
    );
    expect(fakeApi.requestsTo('PUT', '/api/projects/shop/rating-weights')).toHaveLength(1);
    expect(fakeApi.requestsTo('POST', '/api/analyze')).toHaveLength(1);
  });

  // A change made by hand left the recommendations of the previous structure on screen,
  // ready to be applied to modules that no longer exist (#28). Each test shows a card,
  // makes the change, and expects the page to ask for the recommendations again.
  describe('after a change made by hand in the modules panel', () => {
    beforeEach(() => {
      fakeApi.respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ moves: [moveRecommendationFixture()] })
      );
    });

    async function renderPageWithACard() {
      renderPage();
      await screen.findByRole('button', { name: 'Apply move…' });
      fakeApi.respondTo('GET', '/api/recommendations', noRecommendations);
    }

    async function expectTheCardGone() {
      await vi.waitFor(() =>
        expect(screen.queryByRole('button', { name: 'Apply move…' })).toBeNull()
      );
    }

    function sidebar(): HTMLElement {
      return screen.getByRole('complementary', { name: 'Modules' });
    }

    async function openOrders() {
      await userEvent.click(within(sidebar()).getByRole('button', { name: /^orders/ }));
    }

    it('asks for the recommendations again after a rename', async () => {
      fakeApi.respondTo('POST', '/api/module/rename', null);
      await renderPageWithACard();

      await userEvent.click(within(sidebar()).getByRole('button', { name: 'Actions for orders' }));
      await userEvent.click(await screen.findByRole('menuitem', { name: 'Rename' }));
      const field = within(sidebar()).getByRole('textbox', { name: 'New name for orders' });
      await userEvent.clear(field);
      await userEvent.type(field, 'checkout{Enter}');

      await expectTheCardGone();
    });

    it('asks for the recommendations again after a delete', async () => {
      fakeApi.respondTo('DELETE', '/api/module/delete', null);
      await renderPageWithACard();

      await userEvent.click(within(sidebar()).getByRole('button', { name: 'Actions for billing' }));
      await userEvent.click(await screen.findByRole('menuitem', { name: 'Delete…' }));
      await userEvent.click(await screen.findByRole('button', { name: 'Delete module' }));

      await expectTheCardGone();
    });

    it('asks for the recommendations again after a split', async () => {
      fakeApi.respondTo('POST', '/api/module/split', {
        newModules: [
          moduleFixture({ id: 'a', name: 'orders-a' }),
          moduleFixture({ id: 'b', name: 'orders-b' }),
        ],
      });
      await renderPageWithACard();

      await openOrders();
      await userEvent.click(
        within(sidebar()).getByRole('checkbox', { name: 'Select OrderRepository' })
      );
      await userEvent.click(within(sidebar()).getByRole('button', { name: 'Split' }));

      await expectTheCardGone();
    });

    it('shows the new reference class and asks for the recommendations again', async () => {
      const repository = clazzFixture({ id: 'c2', name: 'OrderRepository' });
      fakeApi.respondTo(
        'POST',
        '/api/module/ref-clazzes',
        moduleFixture({ id: 'm1', name: 'orders', refClazzes: [repository] })
      );
      await renderPageWithACard();

      await openOrders();
      await userEvent.click(
        within(sidebar()).getByRole('checkbox', { name: 'Select OrderRepository' })
      );
      await userEvent.click(within(sidebar()).getByRole('button', { name: 'Set as reference' }));

      await expectTheCardGone();
      expect(within(await moduleNode('orders')).getByText('OrderRepository')).toBeInTheDocument();
    });
  });

  it('folds a panel from the top bar, giving its room to the graph', async () => {
    renderPage();
    await moduleNode('orders');
    const toggle = screen.getByRole('button', { name: 'Modules' });

    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await userEvent.click(toggle);

    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('complementary', { name: 'Modules' })).toBeNull();
    expect(screen.getByRole('complementary', { name: 'Recommendations' })).toBeVisible();
  });

  describe('with the command palette', () => {
    function renderWithPalette() {
      return renderWithProviders(
        <CommandPaletteProvider>
          <AnalyzePageWithProvider />
        </CommandPaletteProvider>,
        { route: '/analyze/shop', path: '/analyze/:projectName' }
      );
    }

    async function runCommand(query: string) {
      fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
      await userEvent.type(await screen.findByRole('combobox', { name: 'Search commands' }), query);
      await userEvent.keyboard('{Enter}');
    }

    it('opens a module from its name', async () => {
      renderWithPalette();
      await moduleNode('billing');

      await runCommand('billing');

      const panel = within(screen.getByRole('complementary', { name: 'Modules' }));
      expect(await panel.findByRole('heading', { name: 'billing' })).toBeInTheDocument();
    });

    it('opens a violation in its own tab', async () => {
      fakeApi.respondTo(
        'GET',
        '/api/recommendations',
        recommendationsFixture({ violations: [violationRecommendationFixture()] })
      );
      renderWithPalette();
      await screen.findByRole('tab', { name: /Violations/ });

      await runCommand('fix violation');

      expect(screen.getByRole('tab', { name: /Violations/ })).toHaveAttribute(
        'aria-selected',
        'true'
      );
      await vi.waitFor(() =>
        expect(screen.getByRole('article', { name: /Fix violation/ })).toHaveFocus()
      );
    });
  });
});

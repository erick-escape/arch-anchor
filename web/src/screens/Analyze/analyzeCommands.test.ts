import { describe, expect, it, vi } from 'vitest';
import { bookstoreModules } from '../../test/fixtures/bookstoreFixture';
import {
  moveRecommendationFixture,
  recommendationsFixture,
  violationRecommendationFixture,
} from '../../test/fixtures/recommendationFixtures';
import {
  actionCommandsOf,
  classCommandsOf,
  moduleCommandsOf,
  recommendationCommandsOf,
} from './analyzeCommands';
import { refactoringCardsOf, violationCardsOf } from './recommendations/recommendationCards';

describe('analyze commands', () => {
  const modules = bookstoreModules();

  it('offers each module with its classes and similarity, and shows it when run', () => {
    const showModule = vi.fn();
    const web = moduleCommandsOf(modules, showModule).find(
      (command) => command.label === 'bookstore/web'
    );

    expect(web?.detail).toBe('4 classes · 75.00%');
    web?.run();
    expect(showModule).toHaveBeenCalledWith(
      modules.find((module) => module.name === 'bookstore/web')?.id
    );
  });

  it('offers each class under its module, and shows the module when run', () => {
    const showModule = vi.fn();
    const salesReport = classCommandsOf(modules, showModule).find(
      (command) => command.label === 'SalesReport'
    );

    expect(salesReport?.detail).toBe('bookstore/web · 50.00%');
    salesReport?.run();
    expect(showModule).toHaveBeenCalledOnce();
  });

  it('offers each recommendation with its priority, or as a violation', () => {
    const response = recommendationsFixture({
      moves: [moveRecommendationFixture()],
      violations: [violationRecommendationFixture()],
    });
    const cards = [...refactoringCardsOf(response), ...violationCardsOf(response)];
    const showRecommendation = vi.fn();

    const commands = recommendationCommandsOf(cards, showRecommendation);

    expect(commands.map((command) => [command.label, command.detail])).toEqual([
      ['Move “SalesReport”', 'High priority · 0.56'],
      ['Fix violation in “SalesReport”', 'Violation'],
    ]);
    commands[1].run();
    expect(showRecommendation).toHaveBeenCalledWith(cards[1]);
  });

  it('names the panel toggles by what they will do', () => {
    const actions = {
      exportAcs: vi.fn(),
      openSettings: vi.fn(),
      toggle: vi.fn(),
      open: { modules: true, recommendations: false },
    };

    const labels = actionCommandsOf(actions).map((command) => command.label);

    expect(labels).toEqual([
      'Export ACs',
      'Settings',
      'Hide the modules panel',
      'Show the recommendations panel',
    ]);
  });
});

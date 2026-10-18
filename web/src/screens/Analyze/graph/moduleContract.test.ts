import { describe, expect, it } from 'vitest';
import { bookstoreModules } from '../../../test/fixtures/bookstoreFixture';
import { clazzFixture, moduleFixture } from '../../../test/fixtures/moduleFixtures';
import {
  classStatusOf,
  projectSimilarityOf,
  totalViolationsOf,
  violationsOf,
} from './moduleContract';

const [model, , , web] = bookstoreModules();

describe('violationsOf', () => {
  it('lists the packages a module uses outside its reference classes’ contract', () => {
    expect(violationsOf(web)).toEqual(['java.sql']);
    expect(violationsOf(model)).toEqual([]);
  });
});

describe('totalViolationsOf', () => {
  it('counts the bookstore’s one violation, and none after the move', () => {
    expect(totalViolationsOf(bookstoreModules())).toBe(1);
    expect(totalViolationsOf(bookstoreModules({ afterMove: true }))).toBe(0);
  });
});

describe('projectSimilarityOf', () => {
  // The API's projectSimilarity is the mean of the module similarities (ProjectService).
  it('averages the module similarities, as the API does: 93.75 % for the bookstore', () => {
    expect(projectSimilarityOf(bookstoreModules())).toBeCloseTo(0.9375, 10);
  });

  it('is 0 for no modules', () => {
    expect(projectSimilarityOf([])).toBe(0);
  });
});

describe('classStatusOf', () => {
  it('marks a class that uses a forbidden package', () => {
    const salesReport = web.clazzes.find((clazz) => clazz.name === 'SalesReport')!;

    expect(classStatusOf(salesReport, web)).toBe('divergence');
    expect(classStatusOf(web.clazzes[1], web)).toBeUndefined();
  });

  it('marks a class that lacks a dependency a MUST reference class requires', () => {
    const contract = [{ packageName: 'java.sql', types: [] }];
    const reference = clazzFixture({ id: 'r', enforceMode: 'MUST', dependencies: contract });
    const peer = clazzFixture({ id: 'p', enforceMode: null, dependencies: [] });
    const module = moduleFixture({
      refClazzes: [reference],
      refClazzesDependencies: contract,
      clazzes: [reference, peer],
    });

    expect(classStatusOf(peer, module)).toBe('absence');
    expect(classStatusOf(reference, module)).toBeUndefined();
  });
});

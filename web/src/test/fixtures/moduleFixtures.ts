import { ClazzData } from '../../interface/ClazzData';
import { ModuleData } from '../../interface/ModuleData';

/**
 * Builds a class as the backend serialises it (`ClazzResponseDTO`), with overridable fields.
 *
 * @example
 * const repo = clazzFixture({ id: 'c2', name: 'OrderRepository' });
 */
export function clazzFixture(overrides: Partial<ClazzData> = {}): ClazzData {
  return {
    id: 'c1',
    name: 'OrderService',
    dependencies: [],
    similarity: 0.75,
    avgSimilarityWithRefClazzes: 0.75,
    firstModule: 'm1',
    currentModule: 'm1',
    enforceMode: 'ALLOW',
    ...overrides,
  };
}

/**
 * Builds a module as the backend serialises it (`ModuleDTO`) with one reference class.
 *
 * @example
 * const orders = moduleFixture({ id: 'm1', name: 'orders' });
 */
export function moduleFixture(overrides: Partial<ModuleData> = {}): ModuleData {
  const refClazz = clazzFixture();
  return {
    id: 'm1',
    name: 'orders',
    refClazzes: [refClazz],
    refClazzesDependencies: [],
    allDependencies: [],
    clazzes: [refClazz, clazzFixture({ id: 'c2', name: 'OrderRepository', similarity: 0.5 })],
    similarity: 0.6,
    avgRefClazzesSimilarity: 0.75,
    ...overrides,
  };
}

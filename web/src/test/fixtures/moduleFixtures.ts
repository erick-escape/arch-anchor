import { ClazzData } from '../../interface/ClazzData';
import { ModuleData } from '../../interface/ModuleData';

/**
 * Builds a class as the backend would serialise it, with overridable fields.
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
    firstModule: 'm1',
    currentModule: 'm1',
    enforceMode: 'ALLOW',
    ...overrides,
  };
}

/**
 * Builds a module with one reference class, with overridable fields.
 *
 * @example
 * const orders = moduleFixture({ id: 'm1', name: 'orders' });
 */
export function moduleFixture(overrides: Partial<ModuleData> = {}): ModuleData {
  const refClazz = clazzFixture();
  return {
    id: 'm1',
    name: 'orders',
    refClass: refClazz.name,
    refClazzes: [refClazz],
    clazzes: [refClazz, clazzFixture({ id: 'c2', name: 'OrderRepository', similarity: 0.5 })],
    dependencies: [],
    similarity: 0.6,
    ...overrides,
  };
}

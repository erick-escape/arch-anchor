import { ClazzData } from '../../interface/ClazzData';
import { ModuleData } from '../../interface/ModuleData';

/**
 * Builds a class as the backend serialises it (`ClazzResponseDTO`), with overridable fields.
 * Unless given, its qualified name is the one the Java parser gives a class: package and name
 * joined by a dot, and none in the default package.
 *
 * @example
 * const repo = clazzFixture({ id: 'c2', name: 'OrderRepository' }); // shop.orders.OrderRepository
 */
export function clazzFixture(overrides: Partial<ClazzData> = {}): ClazzData {
  const clazz = {
    id: 'c1',
    name: 'OrderService',
    packageName: 'shop.orders',
    dependencies: [],
    similarity: 0.75,
    avgSimilarityWithRefClazzes: 0.75,
    firstModule: 'm1',
    currentModule: 'm1',
    enforceMode: 'ALLOW' as const,
    ...overrides,
  };
  const qualifiedName = clazz.packageName === null ? null : `${clazz.packageName}.${clazz.name}`;
  return { qualifiedName, ...clazz };
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
    refClazzesChosenByHand: false,
    ...overrides,
  };
}

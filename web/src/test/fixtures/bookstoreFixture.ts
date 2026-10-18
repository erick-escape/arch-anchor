import type { Dependency } from '../../interface/Dependency';
import type { ModuleData } from '../../interface/ModuleData';
import { clazzFixture, moduleFixture } from './moduleFixtures';

// The bookstore's modules as the API analyses them (examples/bookstore/README.md): four layers,
// and SalesReport in web using java.sql, the project's one violation.

function dependency(packageName: string, ...classNames: string[]): Dependency {
  return {
    packageName,
    types: classNames.map((className) => ({
      fullyQualifiedName: `${packageName}.${className}`,
      className,
    })),
  };
}

const MODEL = dependency('bookstore.model', 'Book', 'Customer', 'Order');
const SERVICE = dependency('bookstore.service', 'CatalogService');
const PERSISTENCE = dependency('bookstore.persistence', 'BookRepository');
const SQL = dependency('java.sql', 'Connection', 'DriverManager');
const TIME = dependency('java.time', 'LocalDate');

function layer(id: string, packageName: string, classNames: string[], contract: Dependency[]) {
  const clazzes = classNames.map((name, index) =>
    clazzFixture({
      id: `${id}-c${index}`,
      name,
      packageName,
      dependencies: contract,
      similarity: 1,
    })
  );
  return moduleFixture({
    id,
    name: packageName.replace('.', '/'),
    refClazzes: [clazzes[0]],
    refClazzesDependencies: contract,
    allDependencies: contract,
    clazzes,
    similarity: 1,
  });
}

/**
 * The four bookstore modules. Pass `afterMove` for the modules once SalesReport moved to
 * persistence, which leaves no violation.
 *
 * @example const [model, persistence, service, web] = bookstoreModules();
 */
export function bookstoreModules({ afterMove = false } = {}): ModuleData[] {
  const model = layer('model', 'bookstore.model', ['Book', 'Customer', 'Order'], [TIME]);
  const persistence = layer('persistence', 'bookstore.persistence', persistenceClasses(afterMove), [
    SQL,
    MODEL,
  ]);
  const service = layer(
    'service',
    'bookstore.service',
    ['CatalogService', 'CustomerService', 'OrderService'],
    [MODEL, PERSISTENCE]
  );
  const web = layer(
    'web',
    'bookstore.web',
    ['CatalogController', 'CustomerController', 'OrderController'],
    [SERVICE, MODEL]
  );
  if (afterMove) return [model, persistence, service, web];
  const salesReport = clazzFixture({
    id: 'web-sales',
    name: 'SalesReport',
    packageName: 'bookstore.web',
    dependencies: [SQL, MODEL],
    similarity: 0.5,
  });
  return [
    model,
    persistence,
    service,
    {
      ...web,
      clazzes: [...web.clazzes, salesReport],
      allDependencies: [SQL, SERVICE, MODEL],
      similarity: 0.75,
    },
  ];
}

function persistenceClasses(afterMove: boolean): string[] {
  const repositories = ['BookRepository', 'CustomerRepository', 'OrderRepository'];
  return afterMove ? [...repositories, 'SalesReport'] : repositories;
}

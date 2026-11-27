import type { Dependency } from '../../interface/Dependency';
import type { ModuleData } from '../../interface/ModuleData';
import { simpleTypeName } from '../../utils/simpleTypeName';
import { clazzFixture, moduleFixture } from './moduleFixtures';

// The bookstore's modules as the API analyses them (examples/bookstore/README.md): four layers,
// and SalesReport in web using the database directly, the project's one violation. The PHP
// bookstore (examples/bookstore-php/README.md) gives the same modules under the names PHP writes
// (#85), so the page is also tested on names that are not written with dots.

/** The names one language gives the bookstore's packages and the platform classes it uses. */
interface BookstoreNames {
  /** What joins a package and a class name into the name other classes refer to it by. */
  separator: string;
  model: string;
  persistence: string;
  service: string;
  web: string;
  database: Dependency;
  dates: Dependency;
}

const JAVA: BookstoreNames = {
  separator: '.',
  model: 'bookstore.model',
  persistence: 'bookstore.persistence',
  service: 'bookstore.service',
  web: 'bookstore.web',
  database: platformDependency('java.sql', ['java.sql.Connection', 'java.sql.DriverManager']),
  dates: platformDependency('java.time', ['java.time.LocalDate']),
};

// A class PHP provides is in no namespace: it is named bare and filed under its extension.
const PHP: BookstoreNames = {
  separator: '\\',
  model: 'Bookstore\\Model',
  persistence: 'Bookstore\\Persistence',
  service: 'Bookstore\\Service',
  web: 'Bookstore\\Web',
  database: platformDependency('ext-pdo', ['PDO', 'PDOStatement']),
  dates: platformDependency('ext-date', ['DateTimeImmutable']),
};

const SERVICES = ['CatalogService', 'CustomerService', 'OrderService'];
const CONTROLLERS = ['CatalogController', 'CustomerController', 'OrderController'];

export interface BookstoreOptions {
  /** The modules once SalesReport moved to persistence, which leaves no violation. */
  afterMove?: boolean;
  language?: 'java' | 'php';
}

/**
 * The four bookstore modules, with the ids `model`, `persistence`, `service` and `web` in either
 * language.
 *
 * @example const [model, persistence, service, web] = bookstoreModules();
 * @example bookstoreModules({ language: 'php' })[0].name; // 'Bookstore/Model'
 */
export function bookstoreModules({
  afterMove = false,
  language = 'java',
}: BookstoreOptions = {}): ModuleData[] {
  const names = language === 'php' ? PHP : JAVA;
  const model = projectDependency(names, names.model, ['Book', 'Customer', 'Order']);
  const persistence = projectDependency(names, names.persistence, ['BookRepository']);
  const service = projectDependency(names, names.service, ['CatalogService']);
  const layers = [
    layer(names, 'model', names.model, ['Book', 'Customer', 'Order'], [names.dates]),
    layer(names, 'persistence', names.persistence, persistenceClasses(afterMove), [
      names.database,
      model,
    ]),
    layer(names, 'service', names.service, SERVICES, [model, persistence]),
    layer(names, 'web', names.web, CONTROLLERS, [service, model]),
  ];
  return afterMove ? layers : withSalesReportInWeb(layers, names, model);
}

function projectDependency(
  names: BookstoreNames,
  packageName: string,
  classNames: string[]
): Dependency {
  return {
    packageName,
    types: classNames.map((className) => ({
      fullyQualifiedName: `${packageName}${names.separator}${className}`,
      className,
    })),
  };
}

function platformDependency(packageName: string, fullyQualifiedNames: string[]): Dependency {
  return {
    packageName,
    types: fullyQualifiedNames.map((fullyQualifiedName) => ({
      fullyQualifiedName,
      className: simpleTypeName(fullyQualifiedName),
    })),
  };
}

function layer(
  names: BookstoreNames,
  id: string,
  packageName: string,
  classNames: string[],
  contract: Dependency[]
): ModuleData {
  const clazzes = classNames.map((name, index) =>
    clazzFixture({
      id: `${id}-c${index}`,
      name,
      packageName,
      qualifiedName: `${packageName}${names.separator}${name}`,
      dependencies: contract,
      similarity: 1,
    })
  );
  return moduleFixture({
    id,
    name: packageName.split(names.separator).join('/'),
    refClazzes: [clazzes[0]],
    refClazzesDependencies: contract,
    allDependencies: contract,
    clazzes,
    similarity: 1,
  });
}

function withSalesReportInWeb(
  layers: ModuleData[],
  names: BookstoreNames,
  model: Dependency
): ModuleData[] {
  const [modelLayer, persistence, service, web] = layers;
  const salesReport = clazzFixture({
    id: 'web-sales',
    name: 'SalesReport',
    packageName: names.web,
    qualifiedName: `${names.web}${names.separator}SalesReport`,
    dependencies: [names.database, model],
    similarity: 0.5,
  });
  return [
    modelLayer,
    persistence,
    service,
    {
      ...web,
      clazzes: [...web.clazzes, salesReport],
      allDependencies: [names.database, ...web.allDependencies],
      similarity: 0.75,
    },
  ];
}

function persistenceClasses(afterMove: boolean): string[] {
  const repositories = ['BookRepository', 'CustomerRepository', 'OrderRepository'];
  return afterMove ? [...repositories, 'SalesReport'] : repositories;
}

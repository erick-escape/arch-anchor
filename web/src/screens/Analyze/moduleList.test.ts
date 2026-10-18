import { describe, expect, it } from 'vitest';
import { moduleFixture } from '../../test/fixtures/moduleFixtures';
import {
  withoutIds,
  withRenamedModule,
  withReplacedModule,
  withReplacedModules,
} from './moduleList';

function modulesNamed(count: number) {
  return Array.from({ length: count }, (_, index) =>
    moduleFixture({ id: `m${index}`, name: `module${index}` })
  );
}

describe('moduleList', () => {
  it('drops the listed ids and keeps the rest in order', () => {
    expect(withoutIds(modulesNamed(4), ['m1', 'm3']).map((module) => module.id)).toEqual([
      'm0',
      'm2',
    ]);
  });

  it('renames a module without mutating the previous state', () => {
    const modules = modulesNamed(2);

    const renamed = withRenamedModule(modules, 'm1', 'billing');

    expect(renamed[1].name).toBe('billing');
    expect(modules[1].name).toBe('module1');
  });

  it('puts the changed module in place of the old one', () => {
    const changed = moduleFixture({ id: 'm1', name: 'module1', similarity: 1 });

    const replaced = withReplacedModule(modulesNamed(2), changed);

    expect(replaced[1]).toBe(changed);
  });

  it('swaps merged or split modules for the ones the API returned', () => {
    const merged = moduleFixture({ id: 'm0|m1', name: 'module0 | module1' });

    const result = withReplacedModules(modulesNamed(3), ['m0', 'm1'], [merged]);

    expect(result.map((module) => module.id)).toEqual(['m2', 'm0|m1']);
  });
});

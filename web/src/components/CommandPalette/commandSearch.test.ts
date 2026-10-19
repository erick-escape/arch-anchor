import { describe, expect, it } from 'vitest';
import { searchCommands, type PaletteCommand } from './commandSearch';

function command(group: PaletteCommand['group'], label: string, detail?: string): PaletteCommand {
  return { id: `${group}:${label}`, group, label, detail, run: () => {} };
}

const COMMANDS = [
  command('Actions', 'Export ACs', 'PDF'),
  command('Classes', 'SalesReport', 'bookstore/web · 50.00%'),
  command('Modules', 'bookstore/web', '4 classes · 75.00%'),
  command('Recommendations', 'Move “SalesReport”', 'High 0.56'),
  command('Classes', 'CatalogController', 'bookstore/web · 83.33%'),
];

function labelsOf(groups: ReturnType<typeof searchCommands>): string[][] {
  return groups.map((group) => group.commands.map((found) => found.label));
}

describe('searchCommands', () => {
  it('groups what matches, in the order the palette shows its groups', () => {
    const groups = searchCommands(COMMANDS, 'sales');

    expect(groups.map((group) => group.name)).toEqual(['Classes', 'Recommendations']);
    expect(labelsOf(groups)).toEqual([['SalesReport'], ['Move “SalesReport”']]);
  });

  it('matches every word of the query, in the label or the detail, whatever the case', () => {
    expect(labelsOf(searchCommands(COMMANDS, 'WEB 83'))).toEqual([['CatalogController']]);
  });

  it('puts labels that start with the query first', () => {
    const groups = searchCommands(
      [command('Classes', 'OrderSalesView'), command('Classes', 'SalesReport')],
      'sales'
    );

    expect(labelsOf(groups)).toEqual([['SalesReport', 'OrderSalesView']]);
  });

  it('lists a few of every group before anything is typed', () => {
    const many = Array.from({ length: 9 }, (_, index) => command('Classes', `C${index}`));

    const groups = searchCommands([...many, ...COMMANDS], '', 5);

    expect(groups.find((group) => group.name === 'Classes')?.commands).toHaveLength(5);
    expect(groups.map((group) => group.name)).toEqual([
      'Modules',
      'Classes',
      'Recommendations',
      'Actions',
    ]);
  });

  it('finds nothing for a query nothing contains', () => {
    expect(searchCommands(COMMANDS, 'zzz')).toEqual([]);
  });
});

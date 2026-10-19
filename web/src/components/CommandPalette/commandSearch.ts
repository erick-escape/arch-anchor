/** The groups of the palette, in the order it shows them: what is on screen first. */
export const GROUP_ORDER = [
  'Modules',
  'Classes',
  'Recommendations',
  'Projects',
  'Pages',
  'Actions',
] as const;

export type CommandGroupName = (typeof GROUP_ORDER)[number];

export interface PaletteCommand {
  /** Unique across the palette; also the option's DOM id. */
  id: string;
  group: CommandGroupName;
  label: string;
  /** A second line, also searched: a module's class count, a class's module, a rate. */
  detail?: string;
  run: () => void;
}

export interface CommandGroup {
  name: CommandGroupName;
  commands: PaletteCommand[];
}

/**
 * The commands that contain every word of the query, in their label or detail, grouped in
 * GROUP_ORDER, at most `perGroup` each. Labels that start with the query come first.
 *
 * @example searchCommands(commands, 'sales'); // [{ name: 'Classes', commands: [SalesReport] }, …]
 */
export function searchCommands(
  commands: PaletteCommand[],
  query: string,
  perGroup = 6
): CommandGroup[] {
  const words = query.toLowerCase().split(/\s+/).filter(Boolean);
  const found = commands.filter((command) => matchesAll(command, words));
  return GROUP_ORDER.map((name) => ({
    name,
    commands: byStart(
      found.filter((command) => command.group === name),
      words[0]
    ).slice(0, perGroup),
  })).filter((group) => group.commands.length > 0);
}

function matchesAll(command: PaletteCommand, words: string[]): boolean {
  const text = `${command.label} ${command.detail ?? ''}`.toLowerCase();
  return words.every((word) => text.includes(word));
}

// Stable: commands that tie keep the order they were registered in.
function byStart(commands: PaletteCommand[], firstWord: string | undefined): PaletteCommand[] {
  if (!firstWord) return commands;
  const starts = (command: PaletteCommand) => command.label.toLowerCase().startsWith(firstWord);
  return [...commands.filter(starts), ...commands.filter((command) => !starts(command))];
}

import { createContext, useContext, useEffect } from 'react';
import type { PaletteCommand } from './commandSearch';

export interface PaletteControls {
  open: () => void;
  close: () => void;
  toggle: () => void;
}

export interface PaletteRegistry {
  register: (source: string, commands: PaletteCommand[]) => void;
  unregister: (source: string) => void;
}

// Outside a provider (a screen rendered on its own in a test) the palette is simply absent.
const NO_CONTROLS: PaletteControls = { open: () => {}, close: () => {}, toggle: () => {} };
const NO_REGISTRY: PaletteRegistry = { register: () => {}, unregister: () => {} };

export const PaletteControlsContext = createContext<PaletteControls>(NO_CONTROLS);
export const PaletteRegistryContext = createContext<PaletteRegistry>(NO_REGISTRY);

/**
 * Opens and closes the command palette.
 *
 * @example const palette = useCommandPalette(); <button onClick={palette.open}>⌘K</button>
 */
export function useCommandPalette(): PaletteControls {
  return useContext(PaletteControlsContext);
}

/**
 * Offers a page's commands in the palette while the page is mounted. `commands` should be
 * memoised: every new array re-registers them.
 *
 * @example useRegisterCommands('analyze', useMemo(() => commandsOf(modules), [modules]));
 */
export function useRegisterCommands(source: string, commands: PaletteCommand[]): void {
  const registry = useContext(PaletteRegistryContext);
  useEffect(() => registry.register(source, commands), [registry, source, commands]);
  useEffect(() => () => registry.unregister(source), [registry, source]);
}

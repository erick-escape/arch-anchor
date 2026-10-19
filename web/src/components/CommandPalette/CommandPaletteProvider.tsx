import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { CommandPalette } from './CommandPalette';
import type { PaletteCommand } from './commandSearch';
import {
  PaletteControlsContext,
  PaletteRegistryContext,
  type PaletteRegistry,
} from './paletteContext';
import { isPaletteShortcut } from './shortcutLabel';
import { useGlobalCommands } from './useGlobalCommands';

function useCommandSources(): [PaletteCommand[], PaletteRegistry] {
  const [sources, setSources] = useState<Record<string, PaletteCommand[]>>({});
  const registry = useMemo<PaletteRegistry>(
    () => ({
      register: (source, commands) => setSources((current) => ({ ...current, [source]: commands })),
      unregister: (source) =>
        setSources((current) =>
          Object.fromEntries(Object.entries(current).filter(([key]) => key !== source))
        ),
    }),
    []
  );
  const registered = useMemo(() => Object.values(sources).flat(), [sources]);
  return [registered, registry];
}

/**
 * The command palette of every page, opened with ⌘K or Ctrl+K: the pages' own commands
 * (modules, classes, recommendations on Analyze), then the global ones.
 *
 * @example <BrowserRouter><CommandPaletteProvider><AppRoutes /></CommandPaletteProvider></BrowserRouter>
 */
export function CommandPaletteProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const [registered, registry] = useCommandSources();
  const global = useGlobalCommands(open);
  const controls = useMemo(
    () => ({
      open: () => setOpen(true),
      close: () => setOpen(false),
      toggle: () => setOpen((shown) => !shown),
    }),
    []
  );
  useEffect(() => {
    const toggleOnShortcut = (event: KeyboardEvent) => {
      if (!isPaletteShortcut(event)) return;
      event.preventDefault();
      controls.toggle();
    };
    window.addEventListener('keydown', toggleOnShortcut);
    return () => window.removeEventListener('keydown', toggleOnShortcut);
  }, [controls]);
  const commands = useMemo(() => [...registered, ...global], [registered, global]);
  return (
    <PaletteControlsContext.Provider value={controls}>
      <PaletteRegistryContext.Provider value={registry}>
        {children}
        <CommandPalette open={open} commands={commands} onClose={controls.close} />
      </PaletteRegistryContext.Provider>
    </PaletteControlsContext.Provider>
  );
}

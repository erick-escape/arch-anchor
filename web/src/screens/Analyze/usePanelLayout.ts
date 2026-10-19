import { useCallback, useEffect, useState } from 'react';
import { useMediaQuery } from '../../hooks/useMediaQuery';

export type PanelName = 'modules' | 'recommendations';

export interface PanelLayout {
  /** Below 1024 px the panels are drawers over the graph, closed until asked for. */
  narrow: boolean;
  open: Record<PanelName, boolean>;
  show: (panel: PanelName) => void;
  toggle: (panel: PanelName) => void;
}

const NARROW = '(max-width: 1023px)';

function allPanels(shown: boolean): Record<PanelName, boolean> {
  return { modules: shown, recommendations: shown };
}

/**
 * Which of the Analyze page's panels are open. Both on a wide screen, each foldable; on a
 * narrow one none until asked for, and one at a time, since a drawer covers the graph.
 *
 * @example const layout = usePanelLayout(); <button onClick={() => layout.toggle('modules')} />
 */
export function usePanelLayout(): PanelLayout {
  const narrow = useMediaQuery(NARROW);
  const [open, setOpen] = useState(() => allPanels(!narrow));
  useEffect(() => setOpen(allPanels(!narrow)), [narrow]);
  // A drawer opened on a narrow screen closes the other one.
  const change = useCallback(
    (panel: PanelName, shown: (current: boolean) => boolean) =>
      setOpen((current) => {
        const next = shown(current[panel]);
        return narrow && next
          ? { ...allPanels(false), [panel]: true }
          : { ...current, [panel]: next };
      }),
    [narrow]
  );
  const show = useCallback((panel: PanelName) => change(panel, () => true), [change]);
  const toggle = useCallback((panel: PanelName) => change(panel, (shown) => !shown), [change]);
  return { narrow, open, show, toggle };
}

import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react';
import { ActionsContext, NONE, StateContext, type Flight } from './graphEmphasisContext';

const FLASH_MS = 1600;

/**
 * Lets the recommendations panel point at the graph: hovering a card highlights the modules it
 * touches, and an applied change lands on them. The panel reads only the actions, which never
 * change, so hovering among 1,593 cards (#49) re-renders the graph and not the cards.
 *
 * @example <GraphEmphasisProvider><ModuleGraphCanvas … /><RecommendationsPanel … /></GraphEmphasisProvider>
 */
export function GraphEmphasisProvider({ children }: { children: ReactNode }) {
  const [highlighted, setHighlighted] = useState<ReadonlySet<string>>(NONE);
  const [flashing, setFlashing] = useState<ReadonlySet<string>>(NONE);
  const [flight, setFlight] = useState<Flight | null>(null);
  const flights = useRef(0);
  const highlight = useCallback(
    (ids: string[]) => setHighlighted(ids.length ? new Set(ids) : NONE),
    []
  );
  const land = useCallback((ids: string[], move?: { from: string; to: string }) => {
    setFlashing(new Set(ids));
    if (move) setFlight({ id: ++flights.current, ...move });
    setTimeout(() => setFlashing(NONE), FLASH_MS);
  }, []);
  const actions = useMemo(() => ({ highlight, land }), [highlight, land]);
  const state = useMemo(() => ({ highlighted, flashing, flight }), [highlighted, flashing, flight]);
  return (
    <ActionsContext.Provider value={actions}>
      <StateContext.Provider value={state}>{children}</StateContext.Provider>
    </ActionsContext.Provider>
  );
}

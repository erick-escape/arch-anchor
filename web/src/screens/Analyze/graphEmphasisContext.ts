import { createContext, useContext } from 'react';

/** A class tile travelling from one module to another, the motion of an applied move. */
export interface Flight {
  id: number;
  from: string;
  to: string;
}

export interface GraphEmphasisState {
  /** Nodes the hovered or focused recommendation touches; the graph draws them with a glow. */
  highlighted: ReadonlySet<string>;
  /** Nodes a change just reached; they pulse for a moment. */
  flashing: ReadonlySet<string>;
  flight: Flight | null;
}

export interface GraphEmphasisActions {
  highlight: (nodeIds: string[]) => void;
  /** Shows a change landing: the nodes pulse and, for a move, a tile travels between them. */
  land: (nodeIds: string[], move?: { from: string; to: string }) => void;
}

export const NONE: ReadonlySet<string> = new Set();

export const StateContext = createContext<GraphEmphasisState>({
  highlighted: NONE,
  flashing: NONE,
  flight: null,
});
export const ActionsContext = createContext<GraphEmphasisActions>({
  highlight: () => {},
  land: () => {},
});

/** What the graph should emphasise now. */
export function useGraphEmphasisState(): GraphEmphasisState {
  return useContext(StateContext);
}

/** The setters, stable for the page's life. */
export function useGraphEmphasisActions(): GraphEmphasisActions {
  return useContext(ActionsContext);
}

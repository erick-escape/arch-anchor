/** Whether the OS asks for dark, and when that changes. Injected so tests use a fake. */
export interface ColorSchemeQuery {
  prefersDark(): boolean;
  subscribe(onChange: () => void): () => void;
}

const DARK = '(prefers-color-scheme: dark)';

export class MatchMediaColorSchemeQuery implements ColorSchemeQuery {
  // matchMedia must be called on window, so the default wraps it rather than passing it bare.
  constructor(
    private readonly media: (query: string) => MediaQueryList = (query) => window.matchMedia(query)
  ) {}

  prefersDark(): boolean {
    return this.media(DARK).matches;
  }

  subscribe(onChange: () => void): () => void {
    const list = this.media(DARK);
    list.addEventListener('change', onChange);
    return () => list.removeEventListener('change', onChange);
  }
}

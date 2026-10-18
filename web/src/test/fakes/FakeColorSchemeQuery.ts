import type { ColorSchemeQuery } from '../../theme/ColorSchemeQuery';

/** An OS colour scheme the test can flip, notifying subscribers like matchMedia would. */
export class FakeColorSchemeQuery implements ColorSchemeQuery {
  private readonly listeners = new Set<() => void>();

  constructor(private dark = false) {}

  prefersDark(): boolean {
    return this.dark;
  }

  subscribe(onChange: () => void): () => void {
    this.listeners.add(onChange);
    return () => this.listeners.delete(onChange);
  }

  switchTo(scheme: 'light' | 'dark'): void {
    this.dark = scheme === 'dark';
    this.listeners.forEach((listener) => listener());
  }
}

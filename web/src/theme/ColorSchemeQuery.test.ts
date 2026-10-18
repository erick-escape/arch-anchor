import { describe, expect, it } from 'vitest';
import { MatchMediaColorSchemeQuery } from './ColorSchemeQuery';

class FakeMediaQueryList {
  readonly listeners: Array<() => void> = [];
  constructor(readonly matches: boolean) {}
  addEventListener(_type: string, listener: () => void): void {
    this.listeners.push(listener);
  }
  removeEventListener(_type: string, listener: () => void): void {
    this.listeners.splice(this.listeners.indexOf(listener), 1);
  }
}

describe('MatchMediaColorSchemeQuery', () => {
  it('asks for the dark colour scheme', () => {
    const queries: string[] = [];
    const query = new MatchMediaColorSchemeQuery((media) => {
      queries.push(media);
      return new FakeMediaQueryList(true) as unknown as MediaQueryList;
    });

    expect(query.prefersDark()).toBe(true);
    expect(queries).toEqual(['(prefers-color-scheme: dark)']);
  });

  it('stops notifying once unsubscribed', () => {
    const list = new FakeMediaQueryList(false);
    const query = new MatchMediaColorSchemeQuery(() => list as unknown as MediaQueryList);

    const unsubscribe = query.subscribe(() => {});
    expect(list.listeners).toHaveLength(1);
    unsubscribe();
    expect(list.listeners).toHaveLength(0);
  });
});

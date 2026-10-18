// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../../test/renderWithProviders';
import { useScrollToHash } from './useScrollToHash';

function Sections() {
  useScrollToHash();
  return <section id="how">How it works</section>;
}

describe('useScrollToHash', () => {
  it('scrolls to the section the hash names', () => {
    const scrollIntoView = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoView;

    renderWithProviders(<Sections />, { route: '/#how' });

    expect(scrollIntoView).toHaveBeenCalledOnce();
  });

  it('leaves the page where it is without a hash', () => {
    const scrollIntoView = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoView;

    renderWithProviders(<Sections />, { route: '/' });

    expect(scrollIntoView).not.toHaveBeenCalled();
  });
});

// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SectionHeader from './SectionHeader';

function renderHeader(expanded: boolean, onToggle: () => void = () => {}) {
  render(<SectionHeader title="Dependencies" expanded={expanded} onToggle={onToggle} />);
  return screen.getByRole('button', { name: 'Dependencies' });
}

function isControlCharacter(character: string): boolean {
  const code = character.charCodeAt(0);
  return code < 0x20 || code === 0x7f;
}

describe('SectionHeader', () => {
  it('shows a minus sign and reports itself expanded while the section is open', () => {
    const header = renderHeader(true);

    expect(header).toHaveAttribute('aria-expanded', 'true');
    expect(header).toHaveTextContent('Dependencies−');
  });

  it('shows a plus sign and reports itself collapsed while the section is closed', () => {
    const header = renderHeader(false);

    expect(header).toHaveAttribute('aria-expanded', 'false');
    expect(header).toHaveTextContent('Dependencies+');
  });

  // Regression for #11: the expanded indicator was the control character U+0012, which no
  // font can draw, so it showed as an empty box.
  it.each([true, false])(
    'draws its indicator with a printable character (expanded: %s)',
    (expanded) => {
      const header = renderHeader(expanded);

      const unprintable = [...(header.textContent ?? '')].filter(isControlCharacter);
      expect(unprintable).toEqual([]);
    }
  );

  it('tells its owner when it is clicked', async () => {
    const onToggle = vi.fn();
    const header = renderHeader(false, onToggle);

    await userEvent.click(header);

    expect(onToggle).toHaveBeenCalledOnce();
  });

  it('can be toggled from the keyboard', async () => {
    const onToggle = vi.fn();
    renderHeader(false, onToggle);

    await userEvent.tab();
    await userEvent.keyboard('{Enter}');

    expect(onToggle).toHaveBeenCalledOnce();
  });
});

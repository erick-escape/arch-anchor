// @vitest-environment jsdom
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Logo } from './Logo';
import { Mark } from './Mark';

describe('Mark', () => {
  // Chevron, mountain and base plate are polygons; the arms and nubs are four rotated rectangles.
  it('draws the chevron with its arms, the mountain and the base plate', () => {
    const { container } = render(<Mark size={40} />);

    expect(container.querySelectorAll('polygon')).toHaveLength(3);
    expect(container.querySelectorAll('rect')).toHaveLength(4);
  });

  it('draws only the chevron in mono, for small sizes and the reference-class tile', () => {
    const { container } = render(<Mark size={16} variant="mono" />);

    expect(container.querySelectorAll('polygon')).toHaveLength(1);
    expect(container.querySelectorAll('rect')).toHaveLength(4);
  });

  it('is decorative unless given a title', () => {
    const { container, rerender } = render(<Mark />);
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');

    rerender(<Mark title="ArchAnchor" />);
    expect(screen.getByRole('img', { name: 'ArchAnchor' })).toBeInTheDocument();
  });

  it('keeps the mark’s proportions at any size', () => {
    const { container } = render(<Mark size={104} />);

    expect(container.querySelector('svg')).toHaveAttribute('height', '110');
  });
});

describe('Logo', () => {
  it('writes the wordmark in two tones, with no space between the words', () => {
    render(<Logo />);

    const lockup = screen.getByRole('img', { name: 'ArchAnchor' });
    expect(lockup).toHaveTextContent(/^ArchAnchor$/);
  });

  it('adds the tagline on request', () => {
    render(<Logo tagline />);

    expect(screen.getByText('Architecture grounded in code.')).toBeInTheDocument();
  });

  it('can be the mark alone', () => {
    const { container } = render(<Logo variant="mark" />);

    expect(container).not.toHaveTextContent('Arch');
    expect(screen.getByRole('img', { name: 'ArchAnchor' })).toBeInTheDocument();
  });
});

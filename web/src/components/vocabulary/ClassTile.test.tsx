// @vitest-environment jsdom
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ClassTile } from './ClassTile';

describe('ClassTile', () => {
  it('shows an ordinary class as code with its name under it', () => {
    render(<ClassTile name="OrderController" />);

    expect(screen.getByText('</>')).toBeInTheDocument();
    expect(screen.getByText('OrderController')).toBeInTheDocument();
  });

  it('marks the reference class for screen readers as well as by colour', () => {
    const { container } = render(<ClassTile name="CatalogController" reference />);

    expect(screen.queryByText('</>')).toBeNull();
    expect(container.querySelector('svg')).not.toBeNull();
    expect(screen.getByText('reference class')).toBeInTheDocument();
  });

  it('names a dependency status in words, not only in the outline', () => {
    render(<ClassTile name="SalesReport" status="divergence" />);

    expect(screen.getByText('has a violation')).toBeInTheDocument();
  });

  it('sizes the square from its size', () => {
    const { container } = render(<ClassTile size={28} />);

    expect(container.querySelector('[data-tile]')).toHaveStyle({ width: '28px', height: '28px' });
  });
});

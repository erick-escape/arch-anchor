// @vitest-environment jsdom
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ModuleContainer } from './ModuleContainer';

describe('ModuleContainer', () => {
  it('is a region named by the module, holding its classes', () => {
    render(
      <ModuleContainer name="bookstore/web">
        <span>tile</span>
      </ModuleContainer>
    );

    const module = screen.getByRole('region', { name: 'Module bookstore/web' });
    expect(module).toHaveTextContent('bookstore/web');
    expect(module).toHaveTextContent('tile');
  });

  it('shows its similarity as a meter and counts its violations', () => {
    render(<ModuleContainer name="bookstore/web" similarity={0.75} violations={1} />);

    expect(screen.getByRole('meter', { name: 'Similarity' })).toHaveAttribute(
      'aria-valuenow',
      '0.75'
    );
    expect(screen.getByText('75.00%')).toBeInTheDocument();
    expect(screen.getByText('1 violation')).toBeInTheDocument();
  });

  it('says nothing about violations when there are none', () => {
    render(<ModuleContainer name="bookstore/model" similarity={1} violations={0} />);

    expect(screen.queryByText(/violation/)).toBeNull();
  });

  it('pluralises the violation count', () => {
    render(<ModuleContainer name="shop/web" violations={3} />);

    expect(screen.getByText('3 violations')).toBeInTheDocument();
  });
});

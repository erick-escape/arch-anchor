// @vitest-environment jsdom
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { DependencyArrow } from './DependencyArrow';
import { Meter } from './Meter';
import { ModeToggle } from './ModeToggle';
import { PackageChip } from './PackageChip';
import { PriorityBadge } from './PriorityBadge';
import { StatTile } from './StatTile';

describe('PackageChip', () => {
  it('names the package and, for screen readers, its status', () => {
    render(<PackageChip name="java.sql" status="divergence" />);

    expect(screen.getByText('java.sql')).toBeInTheDocument();
    expect(screen.getByText(', violation')).toBeInTheDocument();
  });
});

describe('Meter', () => {
  it('prints a similarity as a percentage and a rate with two decimals', () => {
    const { rerender } = render(<Meter label="Similarity" value={0.8333} />);
    expect(screen.getByText('83.33%')).toBeInTheDocument();

    rerender(<Meter label="Rate" value={0.5625} format="rate" />);
    expect(screen.getByText('0.56')).toBeInTheDocument();
  });

  it('clamps its fill to the 0..1 range', () => {
    render(<Meter label="Similarity" value={1.4} />);

    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuenow', '1');
  });

  it('is named by its label, or by the name given when the label is shared', () => {
    render(
      <>
        <Meter label="Similarity" value={0.5} />
        <Meter label="Similarity" value={0.8} name="Election: similarity weight" />
      </>
    );

    expect(screen.getByRole('meter', { name: 'Similarity' })).toHaveAttribute(
      'aria-valuenow',
      '0.5'
    );
    expect(screen.getByRole('meter', { name: 'Election: similarity weight' })).toHaveAttribute(
      'aria-valuenow',
      '0.8'
    );
  });
});

describe('StatTile', () => {
  it('shows a headline number above its caption', () => {
    render(<StatTile value="93.75%" caption="project similarity" />);

    expect(screen.getByText('93.75%')).toBeInTheDocument();
    expect(screen.getByText('project similarity')).toBeInTheDocument();
  });
});

describe('PriorityBadge', () => {
  it('says the priority in words, with the rate beside it', () => {
    render(<PriorityBadge level="High" rate={0.625} />);

    expect(screen.getByText('High')).toBeInTheDocument();
    expect(screen.getByText('0.63')).toBeInTheDocument();
  });
});

describe('DependencyArrow', () => {
  it('is an image named by its kind unless given a label', () => {
    const { rerender } = render(<DependencyArrow kind="divergence" />);
    expect(screen.getByRole('img', { name: 'forbidden dependency' })).toBeInTheDocument();

    rerender(<DependencyArrow kind="allow" label="moves to" />);
    expect(screen.getByRole('img', { name: 'moves to' })).toBeInTheDocument();
  });
});

describe('ModeToggle', () => {
  it('offers ALLOW and MUST as radio buttons and explains the mode in force', () => {
    render(<ModeToggle value="ALLOW" onChange={() => {}} label="CatalogController enforce mode" />);

    expect(screen.getByRole('radio', { name: 'ALLOW' })).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('radio', { name: 'MUST' })).toHaveAttribute('aria-checked', 'false');
    expect(screen.getByText(/may use any subset of these dependencies/)).toBeInTheDocument();
  });

  it('reports the mode picked', async () => {
    const onChange = vi.fn();
    render(<ModeToggle value="ALLOW" onChange={onChange} label="mode" />);

    await userEvent.click(screen.getByRole('radio', { name: 'MUST' }));

    expect(onChange).toHaveBeenCalledWith('MUST');
  });

  it('does not report the mode already in force', async () => {
    const onChange = vi.fn();
    render(<ModeToggle value="MUST" onChange={onChange} label="mode" />);

    await userEvent.click(screen.getByRole('radio', { name: 'MUST' }));

    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByText(/must include all of these dependencies/)).toBeInTheDocument();
  });
});

// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Sidebar from './index';
import { fontAwesomeIcon } from '../../test/fontAwesomeIcon';
import { moduleFixture } from '../../test/fixtures/moduleFixtures';

function renderSidebar() {
  return render(
    <Sidebar
      isOpen
      modules={[
        moduleFixture({ id: 'm1', name: 'orders' }),
        moduleFixture({ id: 'm2', name: 'billing' }),
      ]}
      onDeleteRefresh={() => {}}
      onRenameRefresh={() => {}}
      onSplitRefresh={() => {}}
      onRefClazzModeRefresh={() => {}}
    />
  );
}

describe('Sidebar module list', () => {
  it('lists every module as a card', () => {
    renderSidebar();

    expect(screen.getByText('orders')).toBeInTheDocument();
    expect(screen.getByText('billing')).toBeInTheDocument();
  });

  it('asks for confirmation before deleting a module from its card menu', async () => {
    const { container } = renderSidebar();

    await userEvent.click(fontAwesomeIcon(container, 'ellipsis-vertical', 1));
    expect(screen.getByText('Rename')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Delete'));

    expect(screen.getByText('Delete Module')).toBeInTheDocument();
  });
});

describe('Sidebar module detail', () => {
  it('opens a module and offers split and ref-class actions on right click', async () => {
    renderSidebar();

    await userEvent.click(screen.getByText('orders'));
    fireEvent.contextMenu(screen.getByText('OrderRepository'));

    expect(screen.getByRole('heading', { name: 'orders' })).toBeInTheDocument();
    expect(screen.getByText('Set as Ref Classes')).toBeInTheDocument();
    expect(screen.getByText('Split')).toBeInTheDocument();
  });
});

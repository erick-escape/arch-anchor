// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Header from './index';
import { fontAwesomeIcon } from '../../test/fontAwesomeIcon';

function renderHeader(onExportACs: () => void = () => {}) {
  return render(
    <Header projectName="shop" isSidebarOpen onToggleSidebar={() => {}} onExportACs={onExportACs} />
  );
}

describe('Header', () => {
  it('shows the project name', () => {
    renderHeader();

    expect(screen.getByText(/Analyzed Modules for Project: shop/)).toBeInTheDocument();
  });

  it('runs a menu option and closes the menu', async () => {
    const onExportACs = vi.fn();
    const { container } = renderHeader(onExportACs);

    await userEvent.click(fontAwesomeIcon(container, 'ellipsis-vertical'));
    await userEvent.click(screen.getByText('Export ACs'));

    expect(onExportACs).toHaveBeenCalledOnce();
    expect(screen.queryByText('Export ACs')).not.toBeInTheDocument();
  });
});

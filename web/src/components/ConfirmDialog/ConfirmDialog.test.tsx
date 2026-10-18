// @vitest-environment jsdom
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../../test/renderWithProviders';
import { ConfirmDialog } from './ConfirmDialog';

function renderDialog(onConfirm = vi.fn(), onCancel = vi.fn()) {
  renderWithProviders(
    <ConfirmDialog
      open
      title="Delete bookstore?"
      confirmLabel="Delete project"
      destructive
      onConfirm={onConfirm}
      onCancel={onCancel}
    >
      Its sources are removed from this server.
    </ConfirmDialog>
  );
  return { onConfirm, onCancel };
}

describe('ConfirmDialog', () => {
  it('is a dialog that names what it confirms', async () => {
    renderDialog();

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Delete bookstore?');
    expect(dialog).toHaveTextContent('Its sources are removed from this server.');
  });

  it('confirms with the button that says what happens', async () => {
    const { onConfirm } = renderDialog();

    await userEvent.click(await screen.findByRole('button', { name: 'Delete project' }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('cancels', async () => {
    const { onConfirm, onCancel } = renderDialog();

    await userEvent.click(await screen.findByRole('button', { name: 'Cancel' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});

// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AddProjectModal from './index';

describe('AddProjectModal', () => {
  it('opens on the GitHub repository tab', () => {
    render(<AddProjectModal open onClose={() => {}} onSuccess={() => {}} />);

    expect(screen.getByRole('tab', { name: 'GitHub repository' })).toHaveAttribute(
      'aria-selected',
      'true'
    );
    expect(screen.getByLabelText('Repository URL')).toBeInTheDocument();
  });

  it('still offers the local directory upload', async () => {
    render(<AddProjectModal open onClose={() => {}} onSuccess={() => {}} />);

    await userEvent.click(screen.getByRole('tab', { name: 'Local directory' }));

    expect(document.querySelector('input[webkitdirectory]')).not.toBeNull();
  });
});

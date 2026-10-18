// @vitest-environment jsdom
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { bookstoreModules } from '../../test/fixtures/bookstoreFixture';
import { renderWithProviders } from '../../test/renderWithProviders';
import { ExportDialog, type PdfSaver } from './ExportDialog';

/** Records the PDFs the dialog would download. */
class RecordingPdfSaver implements PdfSaver {
  readonly saved: Array<{ projectName: string; fileName: string }> = [];
  save = vi.fn((projectName: string, fileName: string) => {
    this.saved.push({ projectName, fileName });
  });
}

describe('ExportDialog', () => {
  it('previews the constraints grouped by module, numbered as the PDF numbers them', async () => {
    renderWithProviders(
      <ExportDialog
        open
        projectName="bookstore"
        modules={bookstoreModules()}
        onClose={() => {}}
        saver={new RecordingPdfSaver()}
      />
    );

    const dialog = within(await screen.findByRole('dialog'));
    expect(dialog.getByText('Export 7 architectural constraints')).toBeInTheDocument();
    const web = within(dialog.getByRole('region', { name: 'bookstore/web' }));
    expect(web.getByText('CatalogController')).toBeInTheDocument();
    expect(web.getAllByText('CAN-DEPEND')).toHaveLength(2);
    expect(dialog.getByText('AC1')).toBeInTheDocument();
    expect(dialog.getByText('architectural-constraints-bookstore.pdf')).toBeInTheDocument();
  });

  it('downloads the PDF and closes', async () => {
    const saver = new RecordingPdfSaver();
    const onClose = vi.fn();
    renderWithProviders(
      <ExportDialog
        open
        projectName="bookstore"
        modules={bookstoreModules()}
        onClose={onClose}
        saver={saver}
      />
    );

    await userEvent.click(await screen.findByRole('button', { name: 'Download PDF' }));

    expect(saver.saved).toEqual([
      { projectName: 'bookstore', fileName: 'architectural-constraints-bookstore.pdf' },
    ]);
    expect(onClose).toHaveBeenCalled();
  });
});

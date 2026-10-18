import { useEffect, useRef, useState } from 'react';
import type { FolderFile } from '../../../services/projectUploadApi';
import { filesOfDroppedEntries, type FolderEntry } from './droppedFolder';

// Only a drag that carries files is a folder drop; dragging text or a link is left alone.
function carriesFiles(event: DragEvent): boolean {
  return Array.from(event.dataTransfer?.types ?? []).includes('Files');
}

function entriesOf(event: DragEvent): FolderEntry[] {
  return Array.from(event.dataTransfer?.items ?? [])
    .map((item) => item.webkitGetAsEntry())
    .filter((entry): entry is FileSystemEntry => entry !== null);
}

/**
 * Follows a drag of files across the page. Enter and leave fire for every element crossed, so
 * it counts them: the drag has left the page only when the count is back to zero.
 */
class FolderDropTracker {
  private depth = 0;

  constructor(
    private readonly setDragging: (dragging: boolean) => void,
    private readonly onFolder: (files: FolderFile[]) => void
  ) {}

  attach(target: Window): () => void {
    target.addEventListener('dragenter', this.enter);
    target.addEventListener('dragover', this.over);
    target.addEventListener('dragleave', this.leave);
    target.addEventListener('drop', this.drop);
    return () => {
      target.removeEventListener('dragenter', this.enter);
      target.removeEventListener('dragover', this.over);
      target.removeEventListener('dragleave', this.leave);
      target.removeEventListener('drop', this.drop);
    };
  }

  private readonly enter = (event: DragEvent) => {
    if (!carriesFiles(event)) return;
    event.preventDefault();
    this.depth += 1;
    this.setDragging(true);
  };

  // Without this the browser would open the dropped file instead.
  private readonly over = (event: DragEvent) => {
    if (carriesFiles(event)) event.preventDefault();
  };

  private readonly leave = () => {
    this.depth = Math.max(0, this.depth - 1);
    if (this.depth === 0) this.setDragging(false);
  };

  private readonly drop = (event: DragEvent) => {
    if (!carriesFiles(event)) return;
    event.preventDefault();
    this.depth = 0;
    this.setDragging(false);
    void filesOfDroppedEntries(entriesOf(event)).then(this.onFolder);
  };
}

/**
 * Lets a project folder be dropped anywhere on the page. True while one is dragged over it, so
 * the page can say where it will go.
 *
 * @example const dragging = usePageFolderDrop((files) => start({ kind: 'folder', files }));
 */
export function usePageFolderDrop(onFolder: (files: FolderFile[]) => void): boolean {
  const [dragging, setDragging] = useState(false);
  const latest = useRef(onFolder);
  latest.current = onFolder;
  useEffect(
    () => new FolderDropTracker(setDragging, (files) => latest.current(files)).attach(window),
    []
  );
  return dragging;
}

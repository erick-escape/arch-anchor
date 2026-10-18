import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Scrolls to the section the URL's hash names, e.g. the header's "How it works" (/#how). The
 * router changes the URL without the browser's own jump to the anchor.
 *
 * @example useScrollToHash();
 */
export function useScrollToHash(): void {
  const { hash } = useLocation();
  useEffect(() => {
    if (!hash) return;
    document.getElementById(decodeURIComponent(hash.slice(1)))?.scrollIntoView({ block: 'start' });
  }, [hash]);
}

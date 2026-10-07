import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Vitest runs without globals, so Testing Library cannot register its own cleanup.
afterEach(() => {
  cleanup();
});

// antd's Modal measures the scrollbar with getComputedStyle(element, '::-webkit-scrollbar').
// jsdom does not implement the pseudo-element argument and logs a stack trace per modal.
if (typeof window !== 'undefined') {
  const computeStyle = window.getComputedStyle.bind(window);
  window.getComputedStyle = (element: Element) => computeStyle(element);
}

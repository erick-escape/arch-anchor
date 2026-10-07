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

// antd's Table and Grid subscribe to breakpoints through matchMedia, which jsdom lacks. Report
// "no media query matches", i.e. the narrowest layout, and ignore listeners.
if (typeof window !== 'undefined' && !window.matchMedia) {
  window.matchMedia = (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  });
}

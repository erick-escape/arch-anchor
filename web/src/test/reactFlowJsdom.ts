import { vi } from 'vitest';

// React Flow measures nodes with ResizeObserver and reads the viewport zoom from
// DOMMatrixReadOnly; jsdom has neither. These follow React Flow's own testing guide.

class ResizeObserverShim {
  constructor(private readonly onResize: ResizeObserverCallback) {}

  observe(target: Element): void {
    const entry: ResizeObserverEntry = {
      target,
      contentRect: target.getBoundingClientRect(),
      borderBoxSize: [],
      contentBoxSize: [],
      devicePixelContentBoxSize: [],
    };
    this.onResize([entry], this);
  }

  unobserve(): void {}

  disconnect(): void {}
}

class DOMMatrixReadOnlyShim {
  readonly m22: number;

  constructor(transform?: string) {
    const scale = transform?.match(/scale\(([1-9.])\)/)?.[1];
    this.m22 = scale !== undefined ? Number(scale) : 1;
  }
}

/**
 * Lets `<ReactFlow>` mount and render its nodes under jsdom. Call once per test file.
 *
 * @example
 * beforeAll(() => installReactFlowJsdomShims());
 */
export function installReactFlowJsdomShims(): void {
  vi.stubGlobal('ResizeObserver', ResizeObserverShim);
  vi.stubGlobal('DOMMatrixReadOnly', DOMMatrixReadOnlyShim);
  Object.defineProperties(HTMLElement.prototype, {
    offsetHeight: {
      configurable: true,
      get(this: HTMLElement) {
        return parseFloat(this.style.height) || 1;
      },
    },
    offsetWidth: {
      configurable: true,
      get(this: HTMLElement) {
        return parseFloat(this.style.width) || 1;
      },
    },
  });
  Object.defineProperty(SVGElement.prototype, 'getBBox', {
    configurable: true,
    value: () => ({ x: 0, y: 0, width: 0, height: 0 }),
  });
}

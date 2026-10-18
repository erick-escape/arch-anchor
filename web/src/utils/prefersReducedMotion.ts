/**
 * Whether the viewer asked the system for less motion. Motion that carries no information (a
 * travelling tile, a carousel turning on its own) is skipped when it is true.
 *
 * @example if (!prefersReducedMotion()) element.animate(frames, 700);
 */
export function prefersReducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
}

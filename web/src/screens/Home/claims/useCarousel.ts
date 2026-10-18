import { useCallback, useEffect, useState } from 'react';

export interface Carousel {
  index: number;
  /** Held by the pointer or keyboard focus. */
  paused: boolean;
  next: () => void;
  previous: () => void;
  pick: (index: number) => void;
  /** Stops the turning while the pointer or keyboard focus is on the carousel. */
  pause: () => void;
  resume: () => void;
}

interface CarouselTiming {
  intervalMs: number;
  /** True under reduced motion: the carousel then turns only when asked. */
  still: boolean;
}

/**
 * Which of `count` slides is shown. It turns every `intervalMs` unless paused or still, and
 * waits a full interval again after every change, so a slide just chosen stays for the whole
 * interval.
 *
 * @example const claims = useCarousel(5, { intervalMs: 6000, still: prefersReducedMotion() });
 */
export function useCarousel(count: number, { intervalMs, still }: CarouselTiming): Carousel {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const pick = useCallback((chosen: number) => setIndex((chosen + count) % count), [count]);
  useEffect(() => {
    if (still || paused) return;
    const timer = setTimeout(() => pick(index + 1), intervalMs);
    return () => clearTimeout(timer);
  }, [index, paused, still, intervalMs, pick]);
  return {
    index,
    paused,
    next: () => pick(index + 1),
    previous: () => pick(index - 1),
    pick,
    pause: () => setPaused(true),
    resume: () => setPaused(false),
  };
}

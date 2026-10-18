import { faChevronLeft, faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import type { KeyboardEvent } from 'react';
import { prefersReducedMotion } from '../../../utils/prefersReducedMotion';
import { ConstraintsExport, Election, ModulesFound, NoModel, RulesInCode } from './ClaimVignettes';
import styles from './Claims.module.css';
import { useCarousel, type Carousel } from './useCarousel';

const CLAIMS = [
  { text: 'The architecture is already in your code.', Vignette: ModulesFound },
  { text: 'One class per module already carries its rules.', Vignette: Election },
  { text: 'No architectural model required.', Vignette: NoModel },
  { text: 'The rules live in the code.', Vignette: RulesInCode },
  { text: 'A start for the plan you never wrote.', Vignette: ConstraintsExport },
];

const TURN_MS = 6000;

/**
 * The idea in five sentences, each drawn by a vignette of the bookstore. It turns on its own,
 * pauses under the pointer or keyboard focus, takes the arrow keys, and stays still under
 * reduced motion.
 *
 * @example <ClaimsCarousel />
 */
export function ClaimsCarousel() {
  const carousel = useCarousel(CLAIMS.length, {
    intervalMs: TURN_MS,
    still: prefersReducedMotion(),
  });
  const { text, Vignette } = CLAIMS[carousel.index];
  const keys = (event: KeyboardEvent) => {
    if (event.key === 'ArrowLeft') carousel.previous();
    if (event.key === 'ArrowRight') carousel.next();
  };
  return (
    <section
      aria-label="What the reference class shows"
      aria-roledescription="carousel"
      className={styles.band}
      onMouseEnter={carousel.pause}
      onMouseLeave={carousel.resume}
      onFocus={carousel.pause}
      onBlur={carousel.resume}
      onKeyDown={keys}
    >
      <div className={styles.inner}>
        <div className={styles.copy}>
          <span className={styles.counter}>
            Claim {carousel.index + 1} of {CLAIMS.length}
          </span>
          {/* Announced only once the viewer has taken over; a turning carousel would talk every 6 s. */}
          <div aria-live={carousel.paused ? 'polite' : 'off'} className={styles.claimBox}>
            <p key={carousel.index} className={styles.claim}>
              {text}
            </p>
          </div>
          <CarouselControls carousel={carousel} />
        </div>
        <div aria-hidden className={styles.stage}>
          <div key={carousel.index} className={styles.rise}>
            <Vignette />
          </div>
        </div>
      </div>
    </section>
  );
}

function CarouselControls({ carousel }: { carousel: Carousel }) {
  return (
    <div className={styles.controls}>
      <button
        type="button"
        aria-label="Previous claim"
        className={styles.step}
        onClick={carousel.previous}
      >
        <FontAwesomeIcon icon={faChevronLeft} />
      </button>
      <div className={styles.dots}>
        {CLAIMS.map((claim, index) => (
          <button
            key={claim.text}
            type="button"
            aria-label={`Claim ${index + 1} of ${CLAIMS.length}`}
            aria-current={index === carousel.index ? 'true' : undefined}
            className={index === carousel.index ? styles.dotCurrent : styles.dot}
            onClick={() => carousel.pick(index)}
          />
        ))}
      </div>
      <button type="button" aria-label="Next claim" className={styles.step} onClick={carousel.next}>
        <FontAwesomeIcon icon={faChevronRight} />
      </button>
    </div>
  );
}

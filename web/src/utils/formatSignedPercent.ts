// U+2212, the typographic minus: "−16.1%" lines up with "+16.1%", a hyphen does not.
const MINUS_SIGN = '−';

/**
 * A change given as a fraction, printed as a percentage with one decimal and its sign. The
 * cards used to put "+" in front of every value, so a loss read "+-16.1%" (#26). A change
 * that rounds to zero prints without a sign.
 *
 * @example
 * formatSignedPercent(0.125); // '+12.5%'
 * formatSignedPercent(-0.1607); // '−16.1%'
 */
export function formatSignedPercent(fraction: number): string {
  const rounded = (Math.abs(fraction) * 100).toFixed(1);
  if (Number(rounded) === 0) {
    return `${rounded}%`;
  }
  return `${fraction > 0 ? '+' : MINUS_SIGN}${rounded}%`;
}

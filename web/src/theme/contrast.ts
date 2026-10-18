/**
 * WCAG 2.x contrast. The palette test runs every token pair through this, so a colour change
 * that drops text below 4.5:1 or a graphic below 3:1 fails the build.
 *
 * @example contrastRatio('#0E5FB6', '#FFFFFF') // 6.30
 */

const HEX_COLOUR = /^#[0-9a-fA-F]{6}$/;

function channelOf(hex: string, offset: number): number {
  const value = parseInt(hex.slice(offset, offset + 2), 16) / 255;
  return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
}

export function relativeLuminance(hex: string): number {
  if (!HEX_COLOUR.test(hex)) {
    throw new Error(`Expected a colour shaped #rrggbb, got "${hex}"`);
  }
  return 0.2126 * channelOf(hex, 1) + 0.7152 * channelOf(hex, 3) + 0.0722 * channelOf(hex, 5);
}

export function contrastRatio(first: string, second: string): number {
  const [lighter, darker] = [relativeLuminance(first), relativeLuminance(second)].sort(
    (a, b) => b - a
  );
  return (lighter + 0.05) / (darker + 0.05);
}

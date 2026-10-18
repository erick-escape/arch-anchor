import { describe, expect, it } from 'vitest';
import { contrastRatio, relativeLuminance } from './contrast';

describe('relativeLuminance', () => {
  it('is 0 for black and 1 for white', () => {
    expect(relativeLuminance('#000000')).toBe(0);
    expect(relativeLuminance('#FFFFFF')).toBe(1);
  });

  it('rejects a colour that is not #rrggbb', () => {
    expect(() => relativeLuminance('blue')).toThrow(/#rrggbb.*"blue"/);
  });
});

describe('contrastRatio', () => {
  it('is 21 between black and white, whichever comes first', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 5);
    expect(contrastRatio('#FFFFFF', '#000000')).toBeCloseTo(21, 5);
  });

  // The figures quoted in docs/design/redesign-brief.md, section 3.
  it('matches the brief for anchorBlue and mountainBlue on white', () => {
    expect(contrastRatio('#2287F0', '#FFFFFF')).toBeCloseTo(3.63, 2);
    expect(contrastRatio('#0E5FB6', '#FFFFFF')).toBeCloseTo(6.3, 2);
  });
});

import { describe, expect, it } from 'vitest';
import stylesheet from './xy-theme.css?raw';

// The declarations of the first rule whose selector is exactly `selector`.
function declarationsOf(css: string, selector: string): Record<string, string> {
  const withoutComments = css.replace(/\/\*[\s\S]*?\*\//g, '');
  const rule = withoutComments.split('}').find((block) => block.split('{')[0].trim() === selector);
  if (!rule) throw new Error(`Expected a rule for "${selector}" in xy-theme.css`);
  const declarations: Record<string, string> = {};
  for (const line of rule.split('{')[1].split(';')) {
    const [property, ...value] = line.split(':');
    if (value.length > 0) declarations[property.trim()] = value.join(':').trim();
  }
  return declarations;
}

describe('xy-theme.css', () => {
  // A text colour that follows one theme over a background fixed in another gave dark text on
  // a black node in the light colour scheme (#23). Text and backgrounds now come from the same
  // theme's tokens.
  it('takes every node’s text colour, and a plain node’s background, from the theme', () => {
    expect(declarationsOf(stylesheet, '.react-flow__node')['color']).toBe('var(--aa-text)');
    expect(declarationsOf(stylesheet, '.react-flow__node-default')['background-color']).toBe(
      'var(--aa-surface)'
    );
  });

  it('uses no literal colours', () => {
    const withoutComments = stylesheet.replace(/\/\*[\s\S]*?\*\//g, '');
    expect(withoutComments).not.toMatch(/#[0-9a-fA-F]{3,8}\b|rgba?\(|\b(black|white)\b/);
  });
});

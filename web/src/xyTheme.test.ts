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
  // The node's background is fixed, so a text colour that follows the page's colour scheme
  // turns into dark text on a black node whenever the scheme is light (#23).
  it('fixes the text colour of a module node together with its background', () => {
    const node = declarationsOf(stylesheet, '.react-flow__node');

    expect(node['background-color']).toBe('black');
    expect(node['color']).toBe('white');
  });
});

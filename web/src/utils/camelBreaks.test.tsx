// @vitest-environment jsdom
import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { withCamelBreaks } from './camelBreaks';

describe('withCamelBreaks', () => {
  it('lets a class name wrap between its words, not inside one', () => {
    const { container } = render(<span>{withCamelBreaks('CatalogController')}</span>);

    expect(container.querySelector('span')?.innerHTML).toBe('Catalog<wbr>Controller');
  });

  it('keeps an acronym whole and leaves a one-word name alone', () => {
    const { container } = render(
      <>
        <span>{withCamelBreaks('HTTPClientFactory')}</span>
        <span>{withCamelBreaks('Book')}</span>
      </>
    );

    const [acronym, word] = container.querySelectorAll('span');
    expect(acronym.innerHTML).toBe('HTTP<wbr>Client<wbr>Factory');
    expect(word.innerHTML).toBe('Book');
  });
});

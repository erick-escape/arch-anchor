import { Fragment, type ReactNode } from 'react';

// Before an upper-case letter that starts a word: "lC" in CatalogController, "PC" in HTTPClient.
const WORD_START = /(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])/;

/**
 * A class name with a line-break opportunity between its words, so a narrow caption wraps
 * "Catalog / Controller" rather than "CatalogControll / er".
 *
 * @example <span>{withCamelBreaks('CatalogController')}</span> // Catalog<wbr>Controller
 */
export function withCamelBreaks(name: string): ReactNode {
  const words = name.split(WORD_START);
  if (words.length === 1) return name;
  return words.map((word, index) => (
    <Fragment key={index}>
      {index > 0 && <wbr />}
      {word}
    </Fragment>
  ));
}

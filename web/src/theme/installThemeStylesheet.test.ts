// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest';
import { installThemeStylesheet } from './installThemeStylesheet';

describe('installThemeStylesheet', () => {
  afterEach(() => document.getElementById('aa-theme')?.remove());

  it('puts the token variables in the head once, however often it is called', () => {
    installThemeStylesheet(document);
    installThemeStylesheet(document);

    const styles = document.head.querySelectorAll('style#aa-theme');
    expect(styles).toHaveLength(1);
    expect(styles[0].textContent).toContain('--aa-page: #F7FAFE;');
  });
});

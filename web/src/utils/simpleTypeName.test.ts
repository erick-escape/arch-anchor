import { describe, expect, it } from 'vitest';
import { simpleTypeName } from './simpleTypeName';

describe('simpleTypeName', () => {
  it('drops the package of a name written with dots', () => {
    expect(simpleTypeName('java.sql.Connection')).toBe('Connection');
  });

  it('drops the namespace of a name written with backslashes', () => {
    expect(simpleTypeName('App\\Models\\User')).toBe('User');
  });

  it('keeps a name that is in no package as it is', () => {
    expect(simpleTypeName('PDO')).toBe('PDO');
  });

  it('names a nested type by itself, without the class it is declared in', () => {
    expect(simpleTypeName('bookstore.model.Book.Builder')).toBe('Builder');
  });
});

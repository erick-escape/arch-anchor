/**
 * A type's name without the package or namespace it is in: what follows the last `.`, as Java
 * writes names, or the last `\`, as PHP does (#83).
 *
 * @example simpleTypeName('java.sql.Connection') // 'Connection'
 * @example simpleTypeName('App\\Models\\User') // 'User'
 */
export function simpleTypeName(qualifiedName: string): string {
  const lastSeparator = Math.max(qualifiedName.lastIndexOf('.'), qualifiedName.lastIndexOf('\\'));
  return qualifiedName.slice(lastSeparator + 1);
}

/**
 * The three semantic colours mean dependency status and nothing else (brief, section 2.3):
 * conform (a check passed), divergence (a forbidden dependency, the violation) and absence (a
 * MUST dependency that is missing). Each is drawn with a form as well as a colour.
 */
export type DependencyStatus = 'conform' | 'divergence' | 'absence';

/** A package as a module's contract sees it: allowed, or one of the two failures. */
export type PackageStatus = 'allowed' | 'divergence' | 'absence';

export const CLASS_STATUS_WORDS: Record<DependencyStatus, string> = {
  conform: 'conforms',
  divergence: 'has a violation',
  absence: 'misses a MUST dependency',
};

export const PACKAGE_STATUS_WORDS: Record<PackageStatus, string> = {
  allowed: 'allowed',
  divergence: 'violation',
  absence: 'missing (MUST)',
};

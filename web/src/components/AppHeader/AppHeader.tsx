import type { ReactNode } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { Logo } from '../vocabulary/Logo';
import { ThemeSwitch } from '../vocabulary/ThemeSwitch';
import styles from './AppHeader.module.css';

export const REPOSITORY_URL = 'https://github.com/erick-escape/arch-anchor';

interface AppHeaderProps {
  /** Extra controls at the right end, e.g. the command palette trigger. */
  actions?: ReactNode;
}

/**
 * The header of the homepage and the Projects page: the lockup back home, the navigation and
 * the theme switch. The Analyze page has its own compact bar.
 *
 * @example <AppHeader />
 */
export function AppHeader({ actions }: AppHeaderProps) {
  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link to="/" className={styles.home} aria-label="ArchAnchor home">
          <Logo size={34} />
        </Link>
        <nav aria-label="Main" className={styles.nav}>
          <NavLink to="/projects" className={styles.link}>
            Projects
          </NavLink>
          <a href="/#how" className={styles.link}>
            How it works
          </a>
          <a href="/#research" className={styles.link}>
            Research
          </a>
          <a href={REPOSITORY_URL} className={styles.link}>
            GitHub
          </a>
        </nav>
        <div className={styles.actions}>
          <ThemeSwitch />
          {actions}
        </div>
      </div>
    </header>
  );
}

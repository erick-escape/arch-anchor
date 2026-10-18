import { Outlet } from 'react-router-dom';
import { AppHeader } from '../AppHeader/AppHeader';
import styles from './AppShell.module.css';

/**
 * The frame of the homepage and the Projects page: the header, then the page.
 *
 * @example <Route element={<AppShell />}><Route path="/projects" element={<ProjectsPage />} /></Route>
 */
export function AppShell() {
  return (
    <div className={styles.shell}>
      <AppHeader />
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}

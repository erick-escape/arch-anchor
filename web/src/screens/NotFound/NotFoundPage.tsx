import { Link } from 'react-router-dom';
import shell from '../../components/AppShell/AppShell.module.css';
import { ModuleContainer } from '../../components/vocabulary/ModuleContainer';
import styles from './NotFoundPage.module.css';

/**
 * Any URL the app has no page for. Today there was none: an unknown path rendered nothing.
 *
 * @example <Route path="*" element={<NotFoundPage />} />
 */
export function NotFoundPage() {
  return (
    <div className={shell.page}>
      <div className={styles.empty}>
        <div aria-hidden className={styles.illustration}>
          <ModuleContainer name="404" />
        </div>
        <h1 className={shell.heading}>This page does not exist</h1>
        <p className={styles.text}>The address may be mistyped, or the project was deleted.</p>
        <Link to="/" className={styles.home}>
          Go to the homepage
        </Link>
      </div>
    </div>
  );
}

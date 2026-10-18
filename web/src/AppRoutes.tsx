import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell/AppShell';
import AnalyzePageWithProvider from './screens/Analyze';
import { NotFoundPage } from './screens/NotFound/NotFoundPage';
import ProjectsPage from './screens/Projects';

/**
 * The app's pages. Home and Projects share the header of AppShell; Analyze brings its own
 * compact bar so the graph gets the whole screen.
 *
 * @example <BrowserRouter><AppRoutes /></BrowserRouter>
 */
export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        {/* The homepage lands in a later pass of #58; until then "/" is the project list. */}
        <Route path="/" element={<Navigate to="/projects" replace />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="/analyze/:projectName" element={<AnalyzePageWithProvider />} />
    </Routes>
  );
}

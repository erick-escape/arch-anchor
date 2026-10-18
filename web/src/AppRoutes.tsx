import { Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell/AppShell';
import AnalyzePageWithProvider from './screens/Analyze';
import HomePage from './screens/Home';
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
        <Route path="/" element={<HomePage />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="/analyze/:projectName" element={<AnalyzePageWithProvider />} />
    </Routes>
  );
}

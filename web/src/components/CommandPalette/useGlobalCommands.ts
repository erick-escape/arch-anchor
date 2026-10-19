import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useNavigate, type NavigateFunction } from 'react-router-dom';
import type { ProjectSummary } from '../../interface/Project';
import { listProjects } from '../../services/projectsApi';
import { useTheme } from '../../theme/themeContext';
import type { ThemePreference } from '../../theme/themePreference';
import { importSourceLabelOf } from '../../utils/importSourceLabel';
import type { PaletteCommand } from './commandSearch';

const THEME_CHOICES: Array<{ preference: ThemePreference; label: string }> = [
  { preference: 'light', label: 'Switch to the light theme' },
  { preference: 'dark', label: 'Switch to the dark theme' },
  { preference: 'system', label: 'Follow the system theme' },
];

function pageCommands(navigate: NavigateFunction): PaletteCommand[] {
  return [
    {
      id: 'page:home',
      group: 'Pages',
      label: 'Home',
      detail: 'Import a project',
      run: () => navigate('/'),
    },
    {
      id: 'page:projects',
      group: 'Pages',
      label: 'Projects',
      detail: 'All projects',
      run: () => navigate('/projects'),
    },
  ];
}

function projectCommands(projects: ProjectSummary[], navigate: NavigateFunction): PaletteCommand[] {
  return projects.map(({ name, source }) => ({
    id: `project:${name}`,
    group: 'Projects',
    label: `Analyze ${name}`,
    detail: importSourceLabelOf(source),
    run: () => navigate(`/analyze/${encodeURIComponent(name)}`),
  }));
}

/**
 * The commands of every page: go home or to the projects, analyze any project, change the
 * theme. The projects are fetched only while the palette is open.
 *
 * @example const commands = useGlobalCommands(open);
 */
export function useGlobalCommands(open: boolean): PaletteCommand[] {
  const navigate = useNavigate();
  const { preference, setPreference } = useTheme();
  const projects = useQuery({ queryKey: ['projects'], queryFn: listProjects, enabled: open });
  return useMemo(() => {
    const themes: PaletteCommand[] = THEME_CHOICES.filter(
      (choice) => choice.preference !== preference
    ).map((choice) => ({
      id: `theme:${choice.preference}`,
      group: 'Actions',
      label: choice.label,
      run: () => setPreference(choice.preference),
    }));
    return [
      ...pageCommands(navigate),
      ...projectCommands(projects.data ?? [], navigate),
      ...themes,
    ];
  }, [navigate, preference, setPreference, projects.data]);
}

import { ReactFlowProvider } from '@xyflow/react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import RecommendationsSidebar from '../../components/RecommendationsSidebar/index.tsx';
import SettingsModal from '../../components/SettingsModal/index.tsx';
import Sidebar from '../../components/Sidebar/index.tsx';
import type { ModuleData } from '../../interface/ModuleData';
import { architecturalConstraintsOf, generateAcsPdf } from '../../utils/exportACs';
import { AnalysisFailed, AnalyzingState } from './AnalysisStates';
import styles from './Analyze.module.css';
import { AnalyzeTopBar } from './AnalyzeTopBar';
import { GraphHeading } from './GraphOverlays';
import { ModuleGraphCanvas } from './ModuleGraphCanvas';
import { useModuleChanges } from './useModuleChanges';
import { useProjectAnalysis, type ProjectAnalysis } from './useProjectAnalysis';
import { useSavedModulesReload } from './useSavedModulesReload';

/**
 * The analysis of one project: the module graph full-bleed, the modules panel on the left and
 * the recommendations on the right. Opening the page analyzes the project from its sources.
 *
 * @example <Route path="/analyze/:projectName" element={<AnalyzePageWithProvider />} />
 */
const AnalyzePage = () => {
  const { projectName = '' } = useParams();
  const analysis = useProjectAnalysis(projectName);
  const { reloads, reload } = useSavedModulesReload(analysis);
  const [settingsOpen, setSettingsOpen] = useState(false);
  return (
    <div className={styles.page}>
      <AnalyzeTopBar
        projectName={projectName}
        canExport={analysis.status === 'ready' && analysis.modules.length > 0}
        onExport={() => exportConstraints(projectName, analysis.modules)}
        onOpenSettings={() => setSettingsOpen(true)}
      />
      <AnalysisStage
        projectName={projectName}
        analysis={analysis}
        reloads={reloads}
        onReload={reload}
      />
      <SettingsModal
        projectName={projectName}
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        onSaved={reload}
      />
    </div>
  );
};

// Exported from the modules on screen, so the PDF follows every split, merge and move.
function exportConstraints(projectName: string, modules: ModuleData[]): void {
  generateAcsPdf(projectName, architecturalConstraintsOf(modules)).save(
    `architectural-constraints-${projectName}.pdf`
  );
}

interface AnalysisStageProps {
  projectName: string;
  analysis: ProjectAnalysis;
  reloads: number;
  onReload: () => void;
}

function AnalysisStage({ projectName, analysis, reloads, onReload }: AnalysisStageProps) {
  if (analysis.status === 'analyzing') return <AnalyzingState projectName={projectName} />;
  if (analysis.status === 'failed') {
    return (
      <AnalysisFailed
        projectName={projectName}
        reason={analysis.error ?? ''}
        onRetry={analysis.retry}
      />
    );
  }
  return (
    <Workspace
      projectName={projectName}
      analysis={analysis}
      reloads={reloads}
      onReload={onReload}
    />
  );
}

function Workspace({ projectName, analysis, reloads, onReload }: AnalysisStageProps) {
  const changes = useModuleChanges(analysis);
  return (
    <main className={styles.stage}>
      <ModuleGraphCanvas modules={analysis.modules} changes={changes} />
      <GraphHeading projectName={projectName} modules={analysis.modules} />
      <Sidebar key={reloads} isOpen modules={analysis.modules} {...changes} />
      <RecommendationsSidebar
        projectName={projectName}
        analyzed
        onRecommendationApplied={onReload}
      />
    </main>
  );
}

function AnalyzePageWithProvider() {
  return (
    <ReactFlowProvider>
      <AnalyzePage />
    </ReactFlowProvider>
  );
}

export default AnalyzePageWithProvider;

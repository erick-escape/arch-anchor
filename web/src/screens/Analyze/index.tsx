import { ReactFlowProvider } from '@xyflow/react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import SettingsModal from '../../components/SettingsModal/index.tsx';
import type { ModuleData } from '../../interface/ModuleData';
import { architecturalConstraintsOf, generateAcsPdf } from '../../utils/exportACs';
import { AnalysisFailed, AnalyzingState } from './AnalysisStates';
import styles from './Analyze.module.css';
import { AnalyzeTopBar } from './AnalyzeTopBar';
import { GraphEmphasisProvider } from './graphEmphasis';
import { GraphHeading } from './GraphOverlays';
import { ModuleGraphCanvas } from './ModuleGraphCanvas';
import { MergeModulesDialog, type MergeProposal } from './modules/ModuleDialogs';
import { ModulesPanel } from './modules/ModulesPanel';
import { RecommendationsPanel } from './recommendations/RecommendationsPanel';
import { useModuleChanges } from './useModuleChanges';
import { useModuleOperations } from './useModuleOperations';
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
  const reload = useSavedModulesReload(analysis);
  const [settingsOpen, setSettingsOpen] = useState(false);
  return (
    <div className={styles.page}>
      <AnalyzeTopBar
        projectName={projectName}
        canExport={analysis.status === 'ready' && analysis.modules.length > 0}
        onExport={() => exportConstraints(projectName, analysis.modules)}
        onOpenSettings={() => setSettingsOpen(true)}
      />
      <AnalysisStage projectName={projectName} analysis={analysis} onReload={reload} />
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
  onReload: () => void;
}

function AnalysisStage({ projectName, analysis, onReload }: AnalysisStageProps) {
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
  return <Workspace projectName={projectName} analysis={analysis} onReload={onReload} />;
}

function Workspace({ projectName, analysis, onReload }: AnalysisStageProps) {
  const operations = useModuleOperations(useModuleChanges(analysis));
  const [mergeProposal, setMergeProposal] = useState<MergeProposal | null>(null);
  return (
    <GraphEmphasisProvider>
      <main className={styles.stage}>
        <ModuleGraphCanvas
          modules={analysis.modules}
          onProposeMerge={(source, target) => setMergeProposal({ source, target })}
        />
        <GraphHeading projectName={projectName} modules={analysis.modules} />
        <ModulesPanel
          modules={analysis.modules}
          operations={operations}
          onProposeMerge={(source) => setMergeProposal({ source })}
        />
        <RecommendationsPanel projectName={projectName} analyzed onApplied={onReload} />
        <MergeModulesDialog
          proposal={mergeProposal}
          modules={analysis.modules}
          operations={operations}
          onClose={() => setMergeProposal(null)}
        />
      </main>
    </GraphEmphasisProvider>
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

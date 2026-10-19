import { ReactFlowProvider } from '@xyflow/react';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import SettingsModal from '../../components/SettingsModal/index.tsx';
import { AnalysisFailed, AnalyzingState } from './AnalysisStates';
import type { PageActions } from './analyzeCommands';
import styles from './Analyze.module.css';
import { AnalyzeTopBar } from './AnalyzeTopBar';
import { ExportDialog } from './ExportDialog';
import { GraphEmphasisProvider } from './graphEmphasis';
import { GraphHeading } from './GraphOverlays';
import { ModuleGraphCanvas } from './ModuleGraphCanvas';
import { MergeModulesDialog, type MergeProposal } from './modules/ModuleDialogs';
import { ModulesPanel } from './modules/ModulesPanel';
import { RecommendationsPanel } from './recommendations/RecommendationsPanel';
import { useAnalyzeCommands } from './useAnalyzeCommands';
import { useAnalyzeTargets } from './useAnalyzeTargets';
import { useModuleChanges } from './useModuleChanges';
import { useModuleOperations } from './useModuleOperations';
import { useProjectAnalysis, type ProjectAnalysis } from './useProjectAnalysis';
import { usePanelLayout, type PanelLayout } from './usePanelLayout';
import { useSavedModulesReload } from './useSavedModulesReload';

/**
 * The analysis of one project: the module graph full-bleed, the modules panel on the left and
 * the recommendations on the right, each foldable, and drawers below 1024 px. Opening the page
 * analyzes the project from its sources.
 *
 * @example <Route path="/analyze/:projectName" element={<AnalyzePageWithProvider />} />
 */
const AnalyzePage = () => {
  const { projectName = '' } = useParams();
  const analysis = useProjectAnalysis(projectName);
  const reload = useSavedModulesReload(analysis);
  const layout = usePanelLayout();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [exportOpen, setExportOpen] = useState(false);
  const actions = useMemo<PageActions>(
    () => ({
      exportAcs: () => setExportOpen(true),
      openSettings: () => setSettingsOpen(true),
      toggle: layout.toggle,
      open: layout.open,
    }),
    [layout.toggle, layout.open]
  );
  return (
    <div
      className={styles.page}
      data-modules={openness(layout.open.modules)}
      data-recommendations={openness(layout.open.recommendations)}
    >
      <AnalyzeTopBar
        projectName={projectName}
        canExport={analysis.status === 'ready' && analysis.modules.length > 0}
        actions={actions}
      />
      <AnalysisStage
        projectName={projectName}
        analysis={analysis}
        onReload={reload}
        layout={layout}
        actions={actions}
      />
      <SettingsModal
        projectName={projectName}
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        onSaved={reload}
      />
      <ExportDialog
        open={exportOpen}
        projectName={projectName}
        modules={analysis.modules}
        onClose={() => setExportOpen(false)}
      />
    </div>
  );
};

// Read by Analyze.module.css, which gives a folded panel's room back to the graph.
function openness(open: boolean): 'open' | 'closed' {
  return open ? 'open' : 'closed';
}

interface AnalysisStageProps {
  projectName: string;
  analysis: ProjectAnalysis;
  onReload: () => void;
  layout: PanelLayout;
  actions: PageActions;
}

function AnalysisStage(props: AnalysisStageProps) {
  const { projectName, analysis } = props;
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
    <GraphEmphasisProvider>
      <Workspace {...props} />
    </GraphEmphasisProvider>
  );
}

function Workspace({ projectName, analysis, onReload, layout, actions }: AnalysisStageProps) {
  const operations = useModuleOperations(useModuleChanges(analysis));
  const [mergeProposal, setMergeProposal] = useState<MergeProposal | null>(null);
  const targets = useAnalyzeTargets(layout);
  useAnalyzeCommands({ projectName, modules: analysis.modules, targets, actions });
  return (
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
        shownId={targets.shownModuleId}
        onShow={targets.setShownModuleId}
        hidden={!layout.open.modules}
      />
      <RecommendationsPanel
        projectName={projectName}
        analyzed
        onApplied={onReload}
        tab={targets.tab}
        onTabChange={targets.setTab}
        hidden={!layout.open.recommendations}
      />
      <MergeModulesDialog
        proposal={mergeProposal}
        modules={analysis.modules}
        operations={operations}
        onClose={() => setMergeProposal(null)}
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

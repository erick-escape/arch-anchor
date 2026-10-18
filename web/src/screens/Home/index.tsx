import { ClaimsCarousel } from './claims/ClaimsCarousel';
import { DropOverlay } from './DropOverlay';
import { Hero } from './Hero';
import { HowItWorks } from './HowItWorks';
import { useImportFields } from './importBar/useImportFields';
import { usePageFolderDrop } from './importBar/usePageFolderDrop';
import { useProjectImport } from './importBar/useProjectImport';
import { RecentProjects } from './RecentProjects';
import { ResearchBand } from './ResearchBand';
import { useScrollToHash } from './useScrollToHash';

/**
 * The homepage: the import bar first, then the idea in five claims, how it works, the projects
 * already imported and the research behind it. A folder can be dropped anywhere on it.
 *
 * @example <Route path="/" element={<HomePage />} />
 */
const HomePage = () => {
  const fields = useImportFields();
  const projectImport = useProjectImport();
  const dragging = usePageFolderDrop((files) => projectImport.start({ kind: 'folder', files }));
  useScrollToHash();
  return (
    <>
      <Hero fields={fields} projectImport={projectImport} />
      <ClaimsCarousel />
      <HowItWorks />
      <RecentProjects />
      <ResearchBand />
      {dragging && <DropOverlay />}
    </>
  );
};

export default HomePage;

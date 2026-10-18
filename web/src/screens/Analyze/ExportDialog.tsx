import { Button, Modal } from 'antd';
import { useMemo } from 'react';
import { useToast } from '../../components/feedback/useToast';
import { ClassTile } from '../../components/vocabulary/ClassTile';
import type { ModuleData } from '../../interface/ModuleData';
import type { ArchitecturalConstraint } from '../../interface/ProjectAnalyses';
import {
  architecturalConstraintsOf,
  constraintLinesOf,
  generateAcsPdf,
  type ConstraintLine,
} from '../../utils/exportACs';
import styles from './ExportDialog.module.css';

/** Writes the PDF to the viewer's disk. Injected so tests do not download. */
export interface PdfSaver {
  save: (projectName: string, fileName: string, constraints: ArchitecturalConstraint[]) => void;
}

const JSPDF_SAVER: PdfSaver = {
  save: (projectName, fileName, constraints) =>
    generateAcsPdf(projectName, constraints).save(fileName),
};

interface ExportDialogProps {
  open: boolean;
  projectName: string;
  /** The modules on screen, so the export follows every split, merge and move. */
  modules: ModuleData[];
  onClose: () => void;
  saver?: PdfSaver;
}

/**
 * Export ACs: the constraints the PDF will hold, previewed by module, then downloaded. The PDF
 * used to download at once, with no word of what it contained.
 *
 * @example <ExportDialog open projectName="bookstore" modules={modules} onClose={close} />
 */
export function ExportDialog({
  open,
  projectName,
  modules,
  onClose,
  saver = JSPDF_SAVER,
}: ExportDialogProps) {
  const toast = useToast();
  const constraints = useMemo(() => architecturalConstraintsOf(modules), [modules]);
  const lines = useMemo(() => constraintLinesOf(constraints), [constraints]);
  const fileName = `architectural-constraints-${projectName}.pdf`;
  const download = () => {
    saver.save(projectName, fileName, constraints);
    toast.success(`Downloaded ${fileName}`);
    onClose();
  };
  return (
    <Modal
      open={open}
      title={<span className={styles.title}>Export {lines.length} architectural constraints</span>}
      onCancel={onClose}
      width={640}
      centered
      footer={<ExportFooter fileName={fileName} onCancel={onClose} onDownload={download} />}
    >
      <p className={styles.lead}>
        Built from the modules as they are now. One line per dependency of each reference class.
      </p>
      <div className={styles.groups}>
        {constraints.map((module) => (
          <ModuleConstraints
            key={module.moduleId}
            module={module}
            lines={lines.filter((line) => line.moduleId === module.moduleId)}
          />
        ))}
      </div>
    </Modal>
  );
}

function ModuleConstraints({
  module,
  lines,
}: {
  module: ArchitecturalConstraint;
  lines: ConstraintLine[];
}) {
  const references = module.refClassConstraints;
  return (
    <section aria-label={module.moduleName} className={styles.group}>
      <div className={styles.groupHead}>
        <ClassTile reference size={22} />
        <span className={styles.moduleName}>{module.moduleName}</span>
        <span className={styles.references}>
          {references.map((reference) => reference.refClassName).join(' · ')}
        </span>
      </div>
      {lines.map((line) => (
        <div key={line.id} className={styles.line}>
          <span className={styles.lineId}>{line.id}</span>
          <span className={line.keyword === 'MUST-DEPEND' ? styles.must : styles.can}>
            {line.keyword}
          </span>
          <span className={styles.package}>{line.packageName}</span>
        </div>
      ))}
    </section>
  );
}

interface ExportFooterProps {
  fileName: string;
  onCancel: () => void;
  onDownload: () => void;
}

function ExportFooter({ fileName, onCancel, onDownload }: ExportFooterProps) {
  return (
    <div className={styles.footer}>
      <span className={styles.fileName}>{fileName}</span>
      <Button onClick={onCancel}>Cancel</Button>
      <Button type="primary" onClick={onDownload}>
        Download PDF
      </Button>
    </div>
  );
}

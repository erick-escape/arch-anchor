import jsPDF from 'jspdf';
import { ModuleData } from '../interface/ModuleData';
import { ArchitecturalConstraint } from '../interface/ProjectAnalyses';

/**
 * Derives the exportable constraints from the modules as they are now: one per module, taken
 * from its reference classes and their enforce modes. The constraints that come with the
 * analysis describe the modules as first analysed, and go stale with the first split, merge or
 * move (#19).
 *
 * @example
 * generateAcsPdf(projectName, architecturalConstraintsOf(modules));
 */
export function architecturalConstraintsOf(modules: ModuleData[]): ArchitecturalConstraint[] {
  return modules.map((module) => ({
    moduleId: module.id,
    moduleName: module.name,
    refClassConstraints: module.refClazzes.map((refClazz) => ({
      refClassId: refClazz.id,
      refClassName: refClazz.name,
      enforceMode: refClazz.enforceMode ?? 'ALLOW',
      dependencies: refClazz.dependencies,
    })),
  }));
}

export interface ConstraintLine {
  id: string;
  moduleId: string;
  moduleName: string;
  keyword: 'CAN-DEPEND' | 'MUST-DEPEND';
  packageName: string;
  /** The line as the PDF prints it, and as the README and the paper quote it. */
  text: string;
}

/**
 * The constraints as numbered lines, one per dependency of each reference class: ALLOW gives
 * CAN-DEPEND, MUST gives MUST-DEPEND. The PDF and the Export dialog's preview both print these,
 * so they cannot disagree.
 *
 * @example constraintLinesOf(constraints)[0].text // 'AC1 - bookstore/model CAN-DEPEND java.time'
 */
export function constraintLinesOf(constraints: ArchitecturalConstraint[]): ConstraintLine[] {
  const rows = constraints.flatMap((module) =>
    module.refClassConstraints.flatMap((reference) =>
      reference.dependencies.map((dependency) => ({
        moduleId: module.moduleId,
        moduleName: module.moduleName,
        keyword:
          reference.enforceMode === 'MUST' ? ('MUST-DEPEND' as const) : ('CAN-DEPEND' as const),
        packageName: dependency.packageName,
      }))
    )
  );
  return rows.map((row, index) => ({
    id: `AC${index + 1}`,
    ...row,
    text: `AC${index + 1} - ${row.moduleName} ${row.keyword} ${row.packageName}`,
  }));
}

const PAGE_BOTTOM = 275;
const LINE_HEIGHT = 8;
const TEXT_WIDTH = 170;

/**
 * The constraints as a PDF: a title, then one line per constraint. A line too long for the page
 * wraps instead of running off it.
 *
 * @example generateAcsPdf('bookstore', constraints).save('architectural-constraints-bookstore.pdf');
 */
export function generateAcsPdf(projectName: string, constraints: ArchitecturalConstraint[]): jsPDF {
  const doc = new jsPDF();
  doc.setFontSize(16);
  doc.text(`Architectural Constraints for '${projectName}'`, 20, 20);
  doc.setFontSize(12);
  const printed = constraintLinesOf(constraints).flatMap(
    (line) => doc.splitTextToSize(line.text, TEXT_WIDTH) as string[]
  );
  let y = 40;
  for (const part of printed) {
    if (y > PAGE_BOTTOM) {
      doc.addPage();
      y = 20;
    }
    doc.text(part, 20, y);
    y += LINE_HEIGHT;
  }
  return doc;
}

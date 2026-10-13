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

export function generateAcsPdf(projectName: string, constraints: ArchitecturalConstraint[]): jsPDF {
  const doc = new jsPDF();

  doc.setFontSize(16);
  doc.text(`Architectural Constraints for '${projectName}'`, 20, 20);

  doc.setFontSize(12);
  let y = 40;
  let acIndex = 1;

  for (const constraint of constraints) {
    for (const rc of constraint.refClassConstraints) {
      const rule = rc.enforceMode === 'MUST' ? 'MUST-DEPEND' : 'CAN-DEPEND';
      for (const dep of rc.dependencies) {
        if (y > 275) {
          doc.addPage();
          y = 20;
        }
        doc.text(`AC${acIndex} - ${constraint.moduleName} ${rule} ${dep.packageName}`, 20, y);
        y += 8;
        acIndex++;
      }
    }
  }

  return doc;
}

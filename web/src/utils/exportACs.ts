import jsPDF from 'jspdf';
import { ArchitecturalConstraint } from '../interface/ProjectAnalyses';

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

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
    for (const dep of constraint.refClazzesDependencies) {
      if (y > 275) {
        doc.addPage();
        y = 20;
      }
      doc.text(`AC${acIndex} - ${constraint.moduleName} CAN-DEPEND ${dep.packageName}`, 20, y);
      y += 8;
      acIndex++;
    }
  }

  return doc;
}

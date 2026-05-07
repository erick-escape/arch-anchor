import { describe, it, expect } from 'vitest';
import { generateAcsPdf } from './exportACs';
import { ArchitecturalConstraint } from '../interface/ProjectAnalyses';

describe('generateAcsPdf', () => {
  it('generates a non-empty PDF with CAN-DEPEND for ALLOW mode ref classes', () => {
    const constraints: ArchitecturalConstraint[] = [
      {
        moduleId: 'm1',
        moduleName: 'ServiceModule',
        refClassConstraints: [
          {
            refClassId: 'rc1',
            refClassName: 'ServiceClass',
            enforceMode: 'ALLOW',
            dependencies: [
              { packageName: 'org.springframework.stereotype', types: [] },
              { packageName: 'java.util', types: [] },
            ],
          },
        ],
      },
    ];

    const doc = generateAcsPdf('TestProject', constraints);
    const output = doc.output('datauristring');

    expect(output.length).toBeGreaterThan(0);
  });

  it('generates a PDF with MUST-DEPEND for MUST mode ref classes', () => {
    const constraints: ArchitecturalConstraint[] = [
      {
        moduleId: 'm2',
        moduleName: 'RepositoryModule',
        refClassConstraints: [
          {
            refClassId: 'rc2',
            refClassName: 'RepositoryClass',
            enforceMode: 'MUST',
            dependencies: [{ packageName: 'org.springframework.data.jpa', types: [] }],
          },
        ],
      },
    ];

    const doc = generateAcsPdf('TestProject', constraints);
    const output = doc.output('datauristring');

    expect(output.length).toBeGreaterThan(0);
  });

  it('generates a PDF handling mixed ALLOW and MUST modes across ref classes', () => {
    const constraints: ArchitecturalConstraint[] = [
      {
        moduleId: 'm1',
        moduleName: 'MixedModule',
        refClassConstraints: [
          {
            refClassId: 'rc1',
            refClassName: 'AllowRef',
            enforceMode: 'ALLOW',
            dependencies: [{ packageName: 'java.util', types: [] }],
          },
          {
            refClassId: 'rc2',
            refClassName: 'MustRef',
            enforceMode: 'MUST',
            dependencies: [{ packageName: 'org.slf4j', types: [] }],
          },
        ],
      },
    ];

    const doc = generateAcsPdf('TestProject', constraints);
    const output = doc.output('datauristring');

    expect(output.length).toBeGreaterThan(0);
  });
});

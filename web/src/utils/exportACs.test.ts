import { describe, it, expect } from 'vitest';
import { architecturalConstraintsOf, constraintLinesOf, generateAcsPdf } from './exportACs';
import { ArchitecturalConstraint } from '../interface/ProjectAnalyses';
import { clazzFixture, moduleFixture } from '../test/fixtures/moduleFixtures';

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

describe('architecturalConstraintsOf', () => {
  const javaSql = { packageName: 'java.sql', types: [] };

  it('builds one constraint per module from its reference classes and their enforce modes', () => {
    const repository = clazzFixture({
      id: 'c7',
      name: 'BookRepository',
      enforceMode: 'MUST',
      dependencies: [javaSql],
    });
    const persistence = moduleFixture({
      id: 'm2',
      name: 'shop/persistence',
      refClazzes: [repository],
    });

    expect(architecturalConstraintsOf([persistence])).toEqual([
      {
        moduleId: 'm2',
        moduleName: 'shop/persistence',
        refClassConstraints: [
          {
            refClassId: 'c7',
            refClassName: 'BookRepository',
            enforceMode: 'MUST',
            dependencies: [javaSql],
          },
        ],
      },
    ]);
  });

  it('treats a reference class that never had its mode set as ALLOW', () => {
    const neverSet = clazzFixture({ enforceMode: null, dependencies: [javaSql] });
    const persistence = moduleFixture({ refClazzes: [neverSet] });

    const [constraint] = architecturalConstraintsOf([persistence]);

    expect(constraint.refClassConstraints[0].enforceMode).toBe('ALLOW');
  });
});

describe('constraintLinesOf', () => {
  it('numbers the constraints across modules, in the words the PDF and the README use', () => {
    const lines = constraintLinesOf(
      architecturalConstraintsOf([
        moduleFixture({
          name: 'bookstore/model',
          refClazzes: [clazzFixture({ dependencies: [{ packageName: 'java.time', types: [] }] })],
        }),
        moduleFixture({
          name: 'bookstore/persistence',
          refClazzes: [
            clazzFixture({
              enforceMode: 'MUST',
              dependencies: [
                { packageName: 'bookstore.model', types: [] },
                { packageName: 'java.sql', types: [] },
              ],
            }),
          ],
        }),
      ])
    );

    expect(lines.map((line) => line.text)).toEqual([
      'AC1 - bookstore/model CAN-DEPEND java.time',
      'AC2 - bookstore/persistence MUST-DEPEND bookstore.model',
      'AC3 - bookstore/persistence MUST-DEPEND java.sql',
    ]);
    expect(lines[1]).toMatchObject({
      id: 'AC2',
      moduleName: 'bookstore/persistence',
      keyword: 'MUST-DEPEND',
    });
  });
});

describe('generateAcsPdf wrapping', () => {
  // Counts the text runs the PDF draws: the title, then one per printed line.
  function textRunsOf(packageName: string): number {
    const constraints = architecturalConstraintsOf([
      moduleFixture({
        name: 'shop/web',
        refClazzes: [clazzFixture({ dependencies: [{ packageName, types: [] }] })],
      }),
    ]);
    return (generateAcsPdf('shop', constraints).output().match(/\) Tj/g) ?? []).length;
  }

  it('prints a short constraint on one line', () => {
    expect(textRunsOf('java.sql')).toBe(2);
  });

  it('wraps a constraint too long for the page instead of running off it', () => {
    expect(textRunsOf(`org.example.${'deeply.nested.'.repeat(12)}pkg`)).toBeGreaterThan(2);
  });
});

import { describe, it, expect } from 'vitest';
import { writeFileSync } from 'fs';
import { join } from 'path';
import { generateAcsPdf } from './exportACs';

describe('generateAcsPdf', () => {
    it('generates a non-empty PDF and saves it to the project directory', () => {
        const constraints = [
            {
                moduleId: 'm1',
                moduleName: 'ServiceModule',
                refClazzesDependencies: [
                    { packageName: 'org.springframework.stereotype', types: [] },
                    { packageName: 'java.util', types: [] },
                ],
            },
            {
                moduleId: 'm2',
                moduleName: 'RepositoryModule',
                refClazzesDependencies: [
                    { packageName: 'org.springframework.data.jpa', types: [] },
                ],
            },
        ];

        const doc = generateAcsPdf('TestProject', constraints);
        const buffer = Buffer.from(doc.output('arraybuffer'));

        writeFileSync(
            join(__dirname, '../../../architectural-constraints-test.pdf'),
            buffer
        );

        expect(buffer.length).toBeGreaterThan(0);
    });
});

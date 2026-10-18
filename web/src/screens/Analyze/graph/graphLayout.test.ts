import { describe, expect, it } from 'vitest';
import { layeredPositions } from './graphLayout';

const box = (id: string) => ({ id, width: 200, height: 140 });

describe('layeredPositions', () => {
  it('places a dependency below the module that depends on it', () => {
    const positions = layeredPositions(
      [box('web'), box('service'), box('model')],
      [
        { source: 'web', target: 'service' },
        { source: 'service', target: 'model' },
      ]
    );

    expect(positions.get('web')!.y).toBeLessThan(positions.get('service')!.y);
    expect(positions.get('service')!.y).toBeLessThan(positions.get('model')!.y);
  });

  it('gives every node a position, connected or not, and keeps boxes apart', () => {
    const positions = layeredPositions([box('a'), box('b'), box('c')], []);

    expect(positions.size).toBe(3);
    const xs = [...positions.values()].map((position) => position.x).sort((a, b) => a - b);
    expect(xs[1] - xs[0]).toBeGreaterThanOrEqual(200);
  });

  it('returns top-left corners, as React Flow positions nodes', () => {
    const positions = layeredPositions([box('only')], []);

    expect(positions.get('only')).toEqual({ x: 0, y: 0 });
  });
});

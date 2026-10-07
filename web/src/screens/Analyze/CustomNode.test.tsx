// @vitest-environment jsdom
import { beforeAll, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ReactFlow, ReactFlowProvider } from '@xyflow/react';
import CustomNode from './nodeTypes';
import { toModuleNode } from './moduleNodes';
import { ModuleData } from '../../interface/ModuleData';
import { installReactFlowJsdomShims } from '../../test/reactFlowJsdom';
import { clazzFixture, moduleFixture } from '../../test/fixtures/moduleFixtures';

const nodeTypes = { customNode: CustomNode };

function renderNodeFor(module: ModuleData) {
  render(
    <ReactFlowProvider>
      <div style={{ width: 800, height: 600 }}>
        <ReactFlow nodes={[toModuleNode(module, { x: 0, y: 0 })]} nodeTypes={nodeTypes} />
      </div>
    </ReactFlowProvider>
  );
}

describe('CustomNode', () => {
  beforeAll(() => installReactFlowJsdomShims());

  it('shows the module name, its first reference class and its similarity', () => {
    renderNodeFor(
      moduleFixture({
        name: 'orders',
        refClazzes: [clazzFixture({ name: 'OrderService' })],
        similarity: 0.4567,
      })
    );

    expect(screen.getByText('orders')).toBeInTheDocument();
    expect(screen.getByText('OrderService')).toBeInTheDocument();
    expect(screen.getByText('45.67%')).toBeInTheDocument();
  });

  it('shows N/A for a module without reference classes', () => {
    // Regression: `module.refClazzes[0].name` threw for an empty list.
    renderNodeFor(moduleFixture({ refClazzes: [] }));

    expect(screen.getByText('N/A')).toBeInTheDocument();
  });
});

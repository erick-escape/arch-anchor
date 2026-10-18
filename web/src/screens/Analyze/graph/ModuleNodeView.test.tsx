// @vitest-environment jsdom
import { render, screen, within } from '@testing-library/react';
import { ReactFlow, ReactFlowProvider } from '@xyflow/react';
import { beforeAll, describe, expect, it } from 'vitest';
import type { ModuleData } from '../../../interface/ModuleData';
import { bookstoreModules } from '../../../test/fixtures/bookstoreFixture';
import { clazzFixture, moduleFixture } from '../../../test/fixtures/moduleFixtures';
import { installReactFlowJsdomShims } from '../../../test/reactFlowJsdom';
import { graphNodesOf } from './graphElements';
import { dependencyGraphOf } from './moduleEdges';
import { ModuleNodeView } from './ModuleNodeView';
import { PackageNodeView } from './PackageNodeView';

const nodeTypes = { module: ModuleNodeView, package: PackageNodeView };

function renderGraphOf(modules: ModuleData[]) {
  const nodes = graphNodesOf(modules, dependencyGraphOf(modules, { showExternal: false }));
  render(
    <ReactFlowProvider>
      <div style={{ width: 800, height: 600 }}>
        <ReactFlow nodes={nodes} nodeTypes={nodeTypes} />
      </div>
    </ReactFlowProvider>
  );
}

describe('ModuleNodeView', () => {
  beforeAll(() => installReactFlowJsdomShims());

  it('shows the module’s name, reference class and similarity', () => {
    renderGraphOf([
      moduleFixture({
        name: 'orders',
        refClazzes: [clazzFixture({ name: 'OrderService' })],
        similarity: 0.4567,
      }),
    ]);

    const node = screen.getByRole('region', { name: 'Module orders' });
    expect(within(node).getByText('OrderService')).toBeInTheDocument();
    expect(within(node).getByText('45.67%')).toBeInTheDocument();
  });

  it('draws one tile per class, the reference class first and a violating class marked', () => {
    renderGraphOf(bookstoreModules());

    const web = screen.getByRole('region', { name: 'Module bookstore/web' });
    expect(within(web).getAllByText('reference class')).toHaveLength(1);
    expect(within(web).getAllByText('</>')).toHaveLength(3);
    expect(within(web).getByText('has a violation')).toBeInTheDocument();
    expect(within(web).getByText('1 violation')).toBeInTheDocument();
  });

  it('draws the external package of a violation', () => {
    renderGraphOf(bookstoreModules());

    expect(screen.getByText('java.sql')).toBeInTheDocument();
    expect(screen.getByText(', violation')).toBeInTheDocument();
  });

  it('says so when a module has no reference class', () => {
    // Regression: `module.refClazzes[0].name` threw for an empty list.
    renderGraphOf([moduleFixture({ refClazzes: [] })]);

    expect(screen.getByText('no reference class')).toBeInTheDocument();
  });
});

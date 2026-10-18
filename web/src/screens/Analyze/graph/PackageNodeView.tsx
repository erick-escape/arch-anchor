import { Handle, Position, type NodeProps } from '@xyflow/react';
import { PackageChip } from '../../../components/vocabulary/PackageChip';
import type { PackageNode } from './graphElements';
import styles from './graph.module.css';

/**
 * An external package on the graph, drawn as the vocabulary's chip: dashed red when some module
 * uses it outside its contract.
 */
export function PackageNodeView({ data }: NodeProps<PackageNode>) {
  return (
    <>
      <Handle type="target" position={Position.Top} className={styles.handle} />
      <PackageChip name={data.name} status={data.violated ? 'divergence' : 'allowed'} />
    </>
  );
}

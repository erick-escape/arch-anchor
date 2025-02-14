import { Handle, NodeResizeControl, Position } from '@xyflow/react';
import '@xyflow/react/dist/style.css';

const controlStyle = {
    background: 'transparent',
    border: 'none'
};

function ResizeIcon() {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width="20"
            height="20"
            viewBox="0 0 24 24"
            strokeWidth="2"
            stroke="#ffffff"
            fill="none"
            strokeLinecap="round"
            strokeLinejoin="round"
            style={{ position: 'absolute', right: 5, bottom: 5 }}
        >
            <path stroke="none" d="M0 0h24v24H0z" fill="none" />
            <polyline points="16 20 20 20 20 16" />
            <line x1="14" y1="14" x2="20" y2="20" />
            <polyline points="8 4 4 4 4 8" />
            <line x1="4" y1="4" x2="10" y2="10" />
        </svg>
    );
}

function CustomNode({ data }) {
    const { module } = data; // 'module' is what we'll pass from setNodes

    return (
        <div>
            {/* Only show the resizer if this node is selected */}
            {/*{selected && <NodeResizer />}*/}
            <NodeResizeControl style={controlStyle} minWidth={100} minHeight={50}>
                <ResizeIcon />
            </NodeResizeControl>
            <Handle type="target" position={Position.Left} />
            <div style={{ textAlign: 'center', fontWeight: 'bold' }}>
                {module.name}
            </div>
            <div>
                <strong>Ref Class:</strong> {module.refClass || 'N/A'}
            </div>
            <div>
                <strong>Similarity:</strong> {(module.similarity * 100).toFixed(2)}%
            </div>
            <Handle type="source" position={Position.Right} />
        </div>
    );
}

export default CustomNode;
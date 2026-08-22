import React from 'react';
import { MergeData } from './mergeTypes.ts';

// Custom popup component for merge confirmation
export const MergeConfirmPopup: React.FC<MergeData> = ({ source, target, onConfirm, onCancel }) => {
  return (
    <div
      className="merge-confirm-popup"
      style={{
        position: 'absolute',
        top: '20px',
        right: '20px',
        zIndex: 9999,
        background: '#f5f5f5', // Light gray-white mixture
        border: '1px solid #ddd',
        borderRadius: '4px',
        padding: '8px',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.2)',
        width: '180px',
        pointerEvents: 'auto', // Ensure we can click the buttons
      }}
    >
      <p style={{ margin: '0 0 8px 0', fontSize: '12px', textAlign: 'center', color: 'black' }}>
        Merge <strong>{source}</strong> into <strong>{target}</strong>?
      </p>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: '4px' }}>
        <button
          onClick={onCancel}
          style={{
            padding: '4px 8px',
            border: '1px solid #ddd',
            borderRadius: '3px',
            background: '#e0e0e0',
            color: 'black',
            cursor: 'pointer',
            fontSize: '11px',
            flex: '1',
          }}
        >
          Cancel
        </button>
        <button
          onClick={onConfirm}
          style={{
            padding: '4px 8px',
            border: '1px solid #3a803c',
            borderRadius: '3px',
            background: '#4CAF50',
            color: 'white',
            cursor: 'pointer',
            fontSize: '11px',
            flex: '1',
          }}
        >
          Confirm
        </button>
      </div>
    </div>
  );
};

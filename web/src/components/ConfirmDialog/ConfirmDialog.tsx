import { Button, Modal } from 'antd';
import type { ReactNode } from 'react';
import styles from './ConfirmDialog.module.css';

interface ConfirmDialogProps {
  open: boolean;
  /** Names the change, e.g. "Delete bookstore?" or "Move SalesReport to bookstore/persistence?". */
  title: string;
  /** Says exactly what happens, e.g. "Delete project" or "Apply move". */
  confirmLabel: string;
  destructive?: boolean;
  /** Shows a spinner on the confirm button while the change runs. */
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  children?: ReactNode;
}

/**
 * The one confirmation the app uses, for deletes and for applied recommendations alike. antd's
 * Modal gives it a dialog role, a focus trap and Escape.
 *
 * @example
 * <ConfirmDialog open title="Delete bookstore?" confirmLabel="Delete project" destructive
 *   onConfirm={remove} onCancel={close}>Its sources are removed.</ConfirmDialog>
 */
export function ConfirmDialog(props: ConfirmDialogProps) {
  const {
    open,
    title,
    confirmLabel,
    destructive = false,
    busy = false,
    onConfirm,
    onCancel,
  } = props;
  return (
    <Modal
      open={open}
      title={<span className={styles.title}>{title}</span>}
      onCancel={onCancel}
      width={460}
      centered
      footer={[
        <Button key="cancel" onClick={onCancel}>
          Cancel
        </Button>,
        <Button
          key="confirm"
          type={destructive ? 'default' : 'primary'}
          danger={destructive}
          loading={busy}
          onClick={onConfirm}
        >
          {confirmLabel}
        </Button>,
      ]}
    >
      <div className={styles.body}>{props.children}</div>
    </Modal>
  );
}

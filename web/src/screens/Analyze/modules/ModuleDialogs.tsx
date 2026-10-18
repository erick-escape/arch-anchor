import { Radio } from 'antd';
import { useState } from 'react';
import { ConfirmDialog } from '../../../components/ConfirmDialog/ConfirmDialog';
import type { ModuleData } from '../../../interface/ModuleData';
import type { ModuleOperations } from '../useModuleOperations';
import styles from './ModulesPanel.module.css';

interface DeleteModuleDialogProps {
  /** The module to delete; null keeps the dialog closed. */
  module: ModuleData | null;
  operations: ModuleOperations;
  onClose: () => void;
}

/** Deleting a module, confirmed in the one shared dialog. */
export function DeleteModuleDialog({ module, operations, onClose }: DeleteModuleDialogProps) {
  const [busy, setBusy] = useState(false);
  const confirm = async () => {
    if (!module) return;
    setBusy(true);
    const done = await operations.remove(module);
    setBusy(false);
    if (done) onClose();
  };
  return (
    <ConfirmDialog
      open={module !== null}
      title={`Delete ${module?.name}?`}
      confirmLabel="Delete module"
      destructive
      busy={busy}
      onConfirm={confirm}
      onCancel={onClose}
    >
      It leaves this analysis. Opening the project again re-runs the analysis from the sources and
      brings it back.
    </ConfirmDialog>
  );
}

export interface MergeProposal {
  source: ModuleData;
  /** Set when the source was dropped onto it; otherwise the dialog asks. */
  target?: ModuleData;
}

interface MergeModulesDialogProps {
  proposal: MergeProposal | null;
  modules: ModuleData[];
  operations: ModuleOperations;
  onClose: () => void;
}

/**
 * Merging two modules, whether one was dropped onto the other or "Merge into…" was chosen from
 * its menu, in which case the dialog asks into which.
 */
export function MergeModulesDialog({
  proposal,
  modules,
  operations,
  onClose,
}: MergeModulesDialogProps) {
  const [chosenId, setChosenId] = useState<string | undefined>();
  const [busy, setBusy] = useState(false);
  const target = proposal?.target ?? modules.find((module) => module.id === chosenId);
  const close = () => {
    setChosenId(undefined);
    onClose();
  };
  const confirm = async () => {
    if (!proposal || !target) return;
    setBusy(true);
    const done = await operations.merge(proposal.source, target);
    setBusy(false);
    if (done) close();
  };
  const title = target
    ? `Merge ${proposal?.source.name} into ${target.name}?`
    : `Merge ${proposal?.source.name} into…`;
  return (
    <ConfirmDialog
      open={proposal !== null}
      title={title}
      confirmLabel="Merge"
      busy={busy}
      onConfirm={confirm}
      onCancel={close}
    >
      {!proposal?.target && proposal && (
        <MergeTargetChoice
          source={proposal.source}
          modules={modules}
          chosenId={chosenId}
          onChoose={setChosenId}
        />
      )}
      The merged module keeps the reference classes of both.
    </ConfirmDialog>
  );
}

interface MergeTargetChoiceProps {
  source: ModuleData;
  modules: ModuleData[];
  chosenId: string | undefined;
  onChoose: (moduleId: string) => void;
}

function MergeTargetChoice({ source, modules, chosenId, onChoose }: MergeTargetChoiceProps) {
  return (
    <Radio.Group
      aria-label="Merge into"
      value={chosenId}
      onChange={(event) => onChoose(event.target.value)}
      className={styles.targets}
    >
      {modules
        .filter((module) => module.id !== source.id)
        .map((module) => (
          <Radio key={module.id} value={module.id}>
            {module.name}
          </Radio>
        ))}
    </Radio.Group>
  );
}

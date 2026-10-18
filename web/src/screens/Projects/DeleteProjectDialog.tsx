import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ConfirmDialog } from '../../components/ConfirmDialog/ConfirmDialog';
import { useToast } from '../../components/feedback/useToast';
import { deleteProject } from '../../services/projectsApi';

interface DeleteProjectDialogProps {
  /** The project to delete; null keeps the dialog closed. */
  name: string | null;
  onClose: () => void;
}

/**
 * Confirms and runs a project's deletion, then refreshes the list and says what happened.
 *
 * @example <DeleteProjectDialog name={toDelete} onClose={() => setToDelete(null)} />
 */
export function DeleteProjectDialog({ name, onClose }: DeleteProjectDialogProps) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const deletion = useMutation({
    mutationFn: deleteProject,
    onSuccess: (_, deleted) => {
      toast.success(`Deleted ${deleted}`);
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      onClose();
    },
    onError: (error: Error) => toast.error(error.message),
  });
  return (
    <ConfirmDialog
      open={name !== null}
      title={`Delete ${name}?`}
      confirmLabel="Delete project"
      destructive
      busy={deletion.isPending}
      onConfirm={() => name && deletion.mutate(name)}
      onCancel={onClose}
    >
      Its sources and saved analysis are removed from this server. You can import it again later.
    </ConfirmDialog>
  );
}

import { Modal, Tabs } from 'antd';
import RepositoryImportForm from './RepositoryImportForm';
import DirectoryUploadForm from './DirectoryUploadForm';

interface AddProjectModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

// destroyOnClose drops the forms' state when the modal closes, so an access token typed into
// the GitHub tab does not survive a cancelled import.
const AddProjectModal = ({ open, onClose, onSuccess }: AddProjectModalProps) => (
  <Modal open={open} title="Add Project" onCancel={onClose} footer={null} destroyOnClose>
    <Tabs
      defaultActiveKey="github"
      items={[
        {
          key: 'github',
          label: 'GitHub repository',
          children: <RepositoryImportForm onSuccess={onSuccess} />,
        },
        {
          key: 'directory',
          label: 'Local directory',
          children: <DirectoryUploadForm onSuccess={onSuccess} />,
        },
      ]}
    />
  </Modal>
);

export default AddProjectModal;

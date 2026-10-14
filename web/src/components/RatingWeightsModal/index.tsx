import { Modal } from 'antd';
import RatingWeightsForm from './RatingWeightsForm';

interface RatingWeightsModalProps {
  projectName: string;
  open: boolean;
  onClose: () => void;
  // Called once the API has re-elected and re-rated, so the page can reload what it shows.
  onSaved: () => void;
}

// destroyOnClose drops a slider moved but not saved, so the next opening shows the saved weights.
const RatingWeightsModal = ({ projectName, open, onClose, onSaved }: RatingWeightsModalProps) => (
  <Modal open={open} title="Rating weights" onCancel={onClose} footer={null} destroyOnClose>
    <RatingWeightsForm
      projectName={projectName}
      onCancel={onClose}
      onSaved={() => {
        onSaved();
        onClose();
      }}
    />
  </Modal>
);

export default RatingWeightsModal;

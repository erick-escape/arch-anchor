import { Modal, Tabs } from 'antd';
import RatingWeightsForm from '../RatingWeightsForm/RatingWeightsForm';
import AnalysisSettingsForm from '../AnalysisSettingsForm/AnalysisSettingsForm';
import styles from './Settings.module.css';

interface SettingsModalProps {
  projectName: string;
  open: boolean;
  onClose: () => void;
  // Called once the API has re-run what the change affects, so the page can reload what it shows.
  onSaved: () => void;
}

// The project's rating weights (#27) and analysis settings (#36), one tab each, each saved on
// its own. destroyOnClose drops a value changed but not saved, so the next opening shows the
// saved ones.
const SettingsModal = ({ projectName, open, onClose, onSaved }: SettingsModalProps) => {
  const reloadAndClose = () => {
    onSaved();
    onClose();
  };
  return (
    <Modal
      open={open}
      title={<span className={styles.title}>Settings for {projectName}</span>}
      onCancel={onClose}
      footer={null}
      width={640}
      centered
      classNames={{ body: styles.body }}
      destroyOnClose
    >
      <Tabs
        items={[
          {
            key: 'rating-weights',
            label: 'Rating weights',
            children: (
              <RatingWeightsForm
                projectName={projectName}
                onCancel={onClose}
                onSaved={reloadAndClose}
              />
            ),
          },
          {
            // The analysis settings filter what is recommended; the tab used to be called
            // "Recommendations", which named the result rather than the settings.
            key: 'analysis-filters',
            label: 'Analysis filters',
            children: (
              <AnalysisSettingsForm
                projectName={projectName}
                onCancel={onClose}
                onSaved={reloadAndClose}
              />
            ),
          },
        ]}
      />
    </Modal>
  );
};

export default SettingsModal;

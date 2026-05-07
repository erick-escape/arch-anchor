import { useState } from 'react';
import { Button, message, Modal } from 'antd';

const UploadModal = ({ open, onClose, onSuccess }) => {
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);

  const handleFileChange = (event) => {
    const selectedFiles = Array.from(event.target.files);
    setFiles(selectedFiles);
  };

  const uploadFiles = async () => {
    if (!files.length) {
      message.error('Please select a directory to upload');
      return;
    }

    const formData = new FormData();

    files.forEach((file) => {
      formData.append('files', file, file.webkitRelativePath);
    });

    setUploading(true);
    try {
      const response = await fetch('/api/upload', {
        method: 'POST',
        body: formData,
      });

      if (response.ok) {
        message.success('Directory uploaded successfully!');
        onSuccess();
      } else {
        message.error('Failed to upload directory');
      }
    } catch {
      message.error('Error during upload');
    } finally {
      setUploading(false);
    }
  };

  return (
    <Modal
      open={open}
      title="Upload Project"
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose}>
          Cancel
        </Button>,
        <Button key="upload" type="primary" loading={uploading} onClick={uploadFiles}>
          Upload
        </Button>,
      ]}
    >
      {/* eslint-disable-next-line react/no-unknown-property */}
      <input type="file" webkitdirectory="" directory="" multiple onChange={handleFileChange} />
    </Modal>
  );
};

export default UploadModal;

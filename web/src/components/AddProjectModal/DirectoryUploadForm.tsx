import { ChangeEvent, useState } from 'react';
import { Button, Flex, message } from 'antd';

interface DirectoryUploadFormProps {
  onSuccess: () => void;
}

const DirectoryUploadForm = ({ onSuccess }: DirectoryUploadFormProps) => {
  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    setFiles(Array.from(event.target.files ?? []));
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
    <Flex vertical gap="middle" align="flex-start">
      {/* eslint-disable-next-line react/no-unknown-property */}
      <input type="file" webkitdirectory="" directory="" multiple onChange={handleFileChange} />
      <Button type="primary" loading={uploading} onClick={uploadFiles}>
        Upload
      </Button>
    </Flex>
  );
};

export default DirectoryUploadForm;

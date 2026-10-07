import { useState } from 'react';
import { Button, Collapse, Form, FormInstance, Input, message } from 'antd';
import { describeImportFailure, importRepository } from '../../services/projectImportApi';
import { RepositoryImportRequest } from '../../interface/Project';

interface RepositoryImportFormProps {
  onSuccess: () => void;
}

const RepositoryImportForm = ({ onSuccess }: RepositoryImportFormProps) => {
  const [form] = Form.useForm<RepositoryImportRequest>();
  const { importing, submit } = useRepositoryImport(form, onSuccess);

  return (
    <Form form={form} layout="vertical" requiredMark={false} onFinish={submit}>
      <Form.Item
        label="Repository URL"
        name="repositoryUrl"
        rules={[{ required: true, whitespace: true, message: 'Enter the repository URL' }]}
      >
        <Input placeholder="https://github.com/owner/repo" autoComplete="off" />
      </Form.Item>
      <Collapse
        ghost
        size="small"
        items={[{ key: 'options', label: 'Options', children: <ImportOptions /> }]}
      />
      <Button type="primary" htmlType="submit" loading={importing}>
        Import
      </Button>
    </Form>
  );
};

/**
 * Submits the form and reports the outcome. A successful import clears every field, the access
 * token included, so the token does not linger in the form; a failed one keeps them for a retry.
 */
function useRepositoryImport(form: FormInstance<RepositoryImportRequest>, onSuccess: () => void) {
  const [importing, setImporting] = useState(false);

  const submit = async (request: RepositoryImportRequest) => {
    setImporting(true);
    try {
      const imported = await importRepository(request);
      message.success(`Imported ${imported.name} at ${imported.commitSha.slice(0, 7)}`);
      form.resetFields();
      onSuccess();
    } catch (error) {
      message.error(describeImportFailure(error));
    } finally {
      setImporting(false);
    }
  };

  return { importing, submit };
}

const ImportOptions = () => (
  <>
    <Form.Item label="Branch or tag" name="ref">
      <Input placeholder="The repository's default branch" autoComplete="off" />
    </Form.Item>
    <Form.Item
      label="Subdirectory"
      name="subdirectory"
      extra="The folder that holds src/, when it is not the repository root."
    >
      <Input placeholder="backend" autoComplete="off" />
    </Form.Item>
    <Form.Item label="Project name" name="projectName">
      <Input placeholder="The repository name" autoComplete="off" />
    </Form.Item>
    <Form.Item
      label="Access token"
      name="accessToken"
      extra="Only for private repositories. Used once to clone, never stored."
    >
      <Input.Password placeholder="github_pat_…" autoComplete="off" />
    </Form.Item>
  </>
);

export default RepositoryImportForm;

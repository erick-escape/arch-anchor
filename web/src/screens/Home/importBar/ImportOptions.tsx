import { Input } from 'antd';
import { useId, type ReactNode } from 'react';
import type { RepositoryImportRequest } from '../../../interface/Project';
import styles from './ImportBar.module.css';
import type { ImportFields } from './useImportFields';

type OptionKey = Exclude<keyof RepositoryImportRequest, 'repositoryUrl'>;

/**
 * The fields a repository may need, with the help each one used to carry in Add Project.
 *
 * @example {fields.optionsOpen && <ImportOptions fields={fields} />}
 */
export function ImportOptions({ fields }: { fields: ImportFields }) {
  const field = (key: OptionKey) => ({
    value: fields.request[key] ?? '',
    onChange: (value: string) => fields.set({ [key]: value }),
  });
  return (
    <div className={styles.optionGrid}>
      <OptionField
        label="Branch or tag"
        placeholder="The repository's default branch"
        mono
        {...field('ref')}
      />
      <OptionField
        label="Subdirectory"
        placeholder="backend"
        help="The folder that holds src/, when it is not the repository root."
        mono
        {...field('subdirectory')}
      />
      <OptionField
        label="Project name"
        placeholder="The repository name"
        {...field('projectName')}
      />
      <OptionField
        label="Access token"
        placeholder="github_pat_…"
        help="Only for private repositories. Used once to clone, never stored."
        secret
        mono
        {...field('accessToken')}
      />
    </div>
  );
}

interface OptionFieldProps {
  label: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  help?: ReactNode;
  mono?: boolean;
  secret?: boolean;
}

function OptionField({
  label,
  placeholder,
  value,
  onChange,
  help,
  mono,
  secret,
}: OptionFieldProps) {
  const id = useId();
  const Field = secret ? Input.Password : Input;
  return (
    <div className={styles.option}>
      <label htmlFor={id} className={styles.optionLabel}>
        {label}
      </label>
      <Field
        id={id}
        value={value}
        placeholder={placeholder}
        autoComplete="off"
        className={mono ? styles.mono : undefined}
        aria-describedby={help ? `${id}-help` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {help && (
        <span id={`${id}-help`} className={styles.optionHelp}>
          {help}
        </span>
      )}
    </div>
  );
}

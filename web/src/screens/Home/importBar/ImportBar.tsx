import { faCheck, faChevronDown, faFolderOpen, faLink } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useRef, useState, type FormEvent } from 'react';
import { folderFilesOf } from '../../../services/projectUploadApi';
import { githubRepoOf } from './githubRepo';
import { ImportFailure, ImportProgress } from './ImportFeedback';
import styles from './ImportBar.module.css';
import { ImportOptions } from './ImportOptions';
import type { ImportFields } from './useImportFields';
import type { ProjectImport } from './useProjectImport';

const BLANK_URL = 'Paste the URL of a GitHub repository, e.g. https://github.com/owner/repo.';

interface ImportBarProps {
  fields: ImportFields;
  projectImport: ProjectImport;
}

/**
 * The homepage's one action: paste a GitHub URL (Analyze), or upload a folder. The options a
 * repository may need stay folded under Options.
 *
 * @example <ImportBar fields={useImportFields()} projectImport={useProjectImport()} />
 */
export function ImportBar({ fields, projectImport }: ImportBarProps) {
  const [blank, setBlank] = useState(false);
  const failure = blank ? BLANK_URL : projectImport.failure;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const blankNow = fields.request.repositoryUrl.trim() === '';
    setBlank(blankNow);
    if (!blankNow) projectImport.start({ kind: 'repository', request: fields.request });
  };
  return (
    <form
      aria-label="Import a project"
      className={failure ? styles.barFailed : styles.bar}
      onSubmit={submit}
      noValidate
    >
      <div className={styles.row}>
        <RepositoryUrl
          fields={fields}
          onType={() => {
            setBlank(false);
            projectImport.dismiss();
          }}
        />
        <OptionsToggle open={fields.optionsOpen} onToggle={fields.toggleOptions} />
        <FolderPicker onFolder={(files) => projectImport.start({ kind: 'folder', files })} />
        <button
          type="submit"
          className={styles.analyze}
          disabled={projectImport.importing !== null}
        >
          Analyze
        </button>
      </div>
      {fields.optionsOpen && <ImportOptions fields={fields} />}
      {projectImport.importing && <ImportProgress what={projectImport.importing} />}
      {failure && <ImportFailure reason={failure} />}
    </form>
  );
}

function RepositoryUrl({ fields, onType }: { fields: ImportFields; onType: () => void }) {
  const repo = githubRepoOf(fields.request.repositoryUrl);
  return (
    <div className={styles.well}>
      <FontAwesomeIcon icon={faLink} className={styles.wellIcon} />
      <input
        aria-label="GitHub repository URL"
        type="url"
        autoComplete="off"
        autoFocus
        placeholder="https://github.com/owner/repo"
        className={styles.url}
        value={fields.request.repositoryUrl}
        onChange={(event) => {
          onType();
          fields.set({ repositoryUrl: event.target.value });
        }}
      />
      {repo && (
        <span className={styles.repoChip}>
          <FontAwesomeIcon icon={faCheck} className={styles.repoCheck} />
          {repo}
        </span>
      )}
    </div>
  );
}

function OptionsToggle({ open, onToggle }: { open: boolean; onToggle: () => void }) {
  return (
    <button type="button" aria-expanded={open} className={styles.options} onClick={onToggle}>
      Options
      <FontAwesomeIcon
        icon={faChevronDown}
        className={open ? styles.chevronOpen : styles.chevron}
      />
    </button>
  );
}

function FolderPicker({
  onFolder,
}: {
  onFolder: (files: ReturnType<typeof folderFilesOf>) => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  return (
    <>
      <button type="button" className={styles.upload} onClick={() => input.current?.click()}>
        <FontAwesomeIcon icon={faFolderOpen} />
        Upload folder
      </button>
      <input
        ref={input}
        type="file"
        aria-label="Project folder"
        className={styles.folderInput}
        multiple
        {...{ webkitdirectory: '' }}
        onChange={(event) => onFolder(folderFilesOf(Array.from(event.target.files ?? [])))}
      />
    </>
  );
}

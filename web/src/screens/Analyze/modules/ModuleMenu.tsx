import { faEllipsisVertical } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Dropdown, type MenuProps } from 'antd';
import type { ModuleData } from '../../../interface/ModuleData';
import styles from './ModulesPanel.module.css';

/** What a module's ⋮ menu can start. */
export interface ModuleMenuActions {
  onRename: (module: ModuleData) => void;
  onMerge: (module: ModuleData) => void;
  onDelete: (module: ModuleData) => void;
}

interface ModuleMenuProps {
  module: ModuleData;
  actions: ModuleMenuActions;
}

const ITEMS: MenuProps['items'] = [
  { key: 'rename', label: 'Rename' },
  { key: 'merge', label: 'Merge into…' },
  { type: 'divider' },
  { key: 'delete', label: 'Delete…', danger: true },
];

/**
 * A module's ⋮ menu: Rename, Merge into… (the keyboard's way to what dragging does) and Delete…,
 * behind a real, labelled button.
 */
export function ModuleMenu({ module, actions }: ModuleMenuProps) {
  const onClick: MenuProps['onClick'] = ({ key }) => {
    if (key === 'rename') actions.onRename(module);
    if (key === 'merge') actions.onMerge(module);
    if (key === 'delete') actions.onDelete(module);
  };
  return (
    <Dropdown menu={{ items: ITEMS, onClick }} trigger={['click']} placement="bottomRight">
      <button type="button" className={styles.menuButton} aria-label={`Actions for ${module.name}`}>
        <FontAwesomeIcon icon={faEllipsisVertical} />
      </button>
    </Dropdown>
  );
}

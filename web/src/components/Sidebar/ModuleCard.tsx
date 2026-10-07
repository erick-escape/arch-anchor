import { useEffect, useRef, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faCheck,
  faEllipsisVertical,
  faPencil,
  faTimes,
  faTrash,
} from '@fortawesome/free-solid-svg-icons';
import { ModuleCardProps } from './sidebarTypes';
import styles from './Sidebar.module.css';

// Module Card Component
const ModuleCard = ({ module, onClick, onRename, onDelete }: ModuleCardProps) => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isRenaming, setIsRenaming] = useState(false);
  const [newName, setNewName] = useState(module.name);
  const menuRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  useEffect(() => {
    if (isRenaming && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isRenaming]);

  const handleRename = async () => {
    if (newName.trim() && newName !== module.name) {
      try {
        await onRename(module.id, newName);
        setIsRenaming(false);
      } catch (error) {
        console.error('Failed to rename module', error);
      }
    } else {
      // Reset to original name if invalid
      setNewName(module.name);
      setIsRenaming(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleRename();
    } else if (e.key === 'Escape') {
      setNewName(module.name);
      setIsRenaming(false);
    }
  };

  return (
    <div
      className={styles.moduleCard}
      style={{ cursor: isRenaming ? 'default' : 'pointer' }}
      onClick={isRenaming ? undefined : () => onClick(module)}
    >
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {isRenaming ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <input
              ref={inputRef}
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={handleKeyDown}
              style={{
                background: '#333',
                border: 'none',
                color: 'white',
                padding: '5px',
                borderRadius: '3px',
                width: '70%',
              }}
              onClick={(e) => e.stopPropagation()}
            />
            <FontAwesomeIcon
              icon={faCheck}
              onClick={(e) => {
                e.stopPropagation();
                handleRename();
              }}
              style={{
                cursor: 'pointer',
                color: '#4CAF50',
                marginLeft: '5px',
              }}
            />
            <FontAwesomeIcon
              icon={faTimes}
              onClick={(e) => {
                e.stopPropagation();
                setNewName(module.name);
                setIsRenaming(false);
              }}
              style={{
                cursor: 'pointer',
                color: '#ff4d4d',
                marginLeft: '5px',
              }}
            />
          </div>
        ) : (
          <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {module.name}
          </div>
        )}
      </div>

      <div ref={menuRef} style={{ position: 'relative' }}>
        <FontAwesomeIcon
          icon={faEllipsisVertical}
          onClick={(e) => {
            e.stopPropagation();
            setIsMenuOpen(!isMenuOpen);
          }}
          style={{
            cursor: 'pointer',
            padding: '5px',
          }}
        />

        {isMenuOpen && (
          <div
            style={{
              position: 'absolute',
              top: '25px',
              right: '0',
              background: 'black',
              borderRadius: '4px',
              boxShadow: '0 2px 10px rgba(0,0,0,0.2)',
              zIndex: 1000,
              minWidth: '120px',
            }}
          >
            <div
              onClick={(e) => {
                e.stopPropagation();
                setIsMenuOpen(false);
                setIsRenaming(true);
              }}
              className={styles.cardMenuItem}
            >
              <FontAwesomeIcon icon={faPencil} style={{ marginRight: '8px', color: 'white' }} />
              <span>Rename</span>
            </div>
            <div
              onClick={(e) => {
                e.stopPropagation();
                setIsMenuOpen(false);
                onDelete(module.id);
              }}
              className={styles.cardMenuItem}
            >
              <FontAwesomeIcon icon={faTrash} style={{ marginRight: '8px', color: '#ff4d4d' }} />
              <span>Delete</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ModuleCard;

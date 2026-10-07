import { useState, useRef, useEffect } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBars, faEllipsisVertical } from '@fortawesome/free-solid-svg-icons';
import { HeaderProps, HeaderMenuOption } from './headerTypes';
import styles from './Header.module.css';

const Header = ({ projectName, isSidebarOpen, onToggleSidebar, onExportACs }: HeaderProps) => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Example menu options - can be expanded later
  const menuOptions: HeaderMenuOption[] = [
    { label: 'Export ACs', onClick: onExportACs },
    { label: 'Settings', onClick: () => console.log('Settings clicked') },
  ];

  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };

  // Close menu when clicking outside
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

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 20px',
        background: 'black',
        color: 'white',
        width: '100%',
        boxSizing: 'border-box',
        transition: 'width 0.3s ease',
        position: 'relative',
      }}
    >
      <div>
        <FontAwesomeIcon
          icon={faBars}
          onClick={onToggleSidebar}
          style={{
            cursor: 'pointer',
            fontSize: '20px',
            transition: 'transform 0.3s',
            transform: isSidebarOpen ? 'rotate(90deg)' : 'rotate(0)',
          }}
        />
      </div>

      <div style={{ fontSize: '18px', fontWeight: 'bold' }}>
        Analyzed Modules for Project: {projectName}
      </div>

      <div ref={menuRef} style={{ position: 'relative' }}>
        <FontAwesomeIcon
          icon={faEllipsisVertical}
          onClick={toggleMenu}
          style={{ cursor: 'pointer', fontSize: '20px' }}
        />

        {isMenuOpen && (
          <div
            style={{
              position: 'absolute',
              top: '30px',
              right: '0',
              background: 'black',
              boxShadow: '0px 4px 10px rgba(0,0,0,0.2)',
              borderRadius: '4px',
              padding: '8px 0',
              zIndex: 1000,
              minWidth: '120px',
            }}
          >
            {menuOptions.map((option, index) => (
              <div
                key={index}
                onClick={() => {
                  option.onClick();
                  setIsMenuOpen(false);
                }}
                className={styles.menuItem}
              >
                {option.label}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Header;

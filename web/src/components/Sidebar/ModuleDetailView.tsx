import { useEffect, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChevronLeft } from '@fortawesome/free-solid-svg-icons';
import { ModuleDetailViewProps } from './sidebarTypes';
import { ClazzData } from '../../interface/ClazzData.tsx';
import styles from './Sidebar.module.css';

type DetailSection = 'refClazzes' | 'clazzes' | 'dependencies';

// Module Detail View
const ModuleDetailView = ({
  module,
  onBack,
  onSplit,
  onSetRefClazzes,
  onSetRefClazzMode,
}: ModuleDetailViewProps) => {
  const [selectedClazzes, setSelectedClazzes] = useState<string[]>([]);
  const [showContextMenu, setShowContextMenu] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });
  const [expandedSections, setExpandedSections] = useState<Record<DetailSection, boolean>>({
    refClazzes: true,
    clazzes: true,
    dependencies: false,
  });

  const handleClassClick = (classId: string) => {
    setSelectedClazzes((prev) => {
      if (prev.includes(classId)) {
        return prev.filter((id) => id !== classId);
      } else {
        return [...prev, classId];
      }
    });
  };

  const handleRightClick = (e: React.MouseEvent, classId?: string) => {
    e.preventDefault();

    // If right-clicking on a class that's not already selected, add it to selection
    if (classId && !selectedClazzes.includes(classId)) {
      setSelectedClazzes((prev) => [...prev, classId]);
    }

    // Show context menu if we have selected classes or if clicking on a class
    if (selectedClazzes.length > 0 || classId) {
      setMenuPosition({ x: e.clientX, y: e.clientY });
      setShowContextMenu(true);
    }
  };

  const handleSplit = async () => {
    try {
      await onSplit(module.id, selectedClazzes);
      setSelectedClazzes([]);
      setShowContextMenu(false);
    } catch (error) {
      console.error('Failed to split module', error);
    }
  };

  const handleSetRefClazzes = async () => {
    try {
      await onSetRefClazzes(module.id, selectedClazzes);
      setSelectedClazzes([]);
      setShowContextMenu(false);
    } catch (error) {
      console.error('Failed to set reference classes', error);
    }
  };

  useEffect(() => {
    const handleClickOutside = () => {
      setShowContextMenu(false);
    };

    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, []);

  const toggleSection = (section: DetailSection) => {
    setExpandedSections((prev) => ({
      ...prev,
      [section]: !prev[section],
    }));
  };

  return (
    <div
      style={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        color: 'white',
      }}
    >
      <div
        style={{
          padding: '15px',
          display: 'flex',
          alignItems: 'center',
          borderBottom: '1px solid #333',
        }}
      >
        <FontAwesomeIcon
          icon={faChevronLeft}
          onClick={onBack}
          style={{
            cursor: 'pointer',
            marginRight: '10px',
            fontSize: '14px',
          }}
        />
        <h3 style={{ margin: 0 }}>{module.name}</h3>
      </div>

      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '15px',
        }}
      >
        {/* Reference Classes Section */}
        {module.refClazzes && module.refClazzes.length > 0 && (
          <div style={{ marginBottom: '20px' }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                cursor: 'pointer',
                padding: '5px 0',
                borderBottom: '1px solid #333',
                marginBottom: expandedSections.refClazzes ? '10px' : '0',
              }}
              onClick={() => toggleSection('refClazzes')}
            >
              <div style={{ fontWeight: 'bold' }}>Reference Classes</div>
              <div>{expandedSections.refClazzes ? '' : '+'}</div>
            </div>

            {expandedSections.refClazzes && (
              <div
                style={{
                  background: 'black',
                  borderRadius: '4px',
                }}
              >
                {module.refClazzes.map((classItem: ClazzData) => (
                  <div
                    key={classItem.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '8px 10px',
                      color: '#4CAF50',
                      borderRadius: '4px',
                      marginBottom: '2px',
                    }}
                  >
                    <div style={{ flex: 1 }}>{classItem.name}</div>
                    <div style={{ marginRight: '8px', fontSize: '12px' }}>
                      {(classItem.similarity * 100).toFixed(2)}%
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        const nextMode = classItem.enforceMode === 'MUST' ? 'ALLOW' : 'MUST';
                        onSetRefClazzMode(module.id, classItem.id, nextMode);
                      }}
                      title={
                        classItem.enforceMode === 'MUST'
                          ? 'MUST mode: peer classes must include all these dependencies. Click to switch to ALLOW.'
                          : 'ALLOW mode: peer classes may use any subset of these dependencies. Click to switch to MUST.'
                      }
                      style={{
                        padding: '2px 8px',
                        border: 'none',
                        borderRadius: '4px',
                        backgroundColor: classItem.enforceMode === 'MUST' ? '#e67e22' : '#27ae60',
                        color: 'white',
                        fontSize: '11px',
                        fontWeight: 'bold',
                        cursor: 'pointer',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {classItem.enforceMode === 'MUST' ? 'MUST' : 'ALLOW'}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        <div style={{ marginBottom: '20px' }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              cursor: 'pointer',
              padding: '5px 0',
              borderBottom: '1px solid #333',
              marginBottom: expandedSections.clazzes ? '10px' : '0',
            }}
            onClick={() => toggleSection('clazzes')}
          >
            <div style={{ fontWeight: 'bold' }}>Clazzes</div>
            <div>{expandedSections.clazzes ? '' : '+'}</div>
          </div>

          {expandedSections.clazzes && (
            <div
              style={{
                background: 'black',
                borderRadius: '4px',
              }}
            >
              {module.clazzes.map((classItem: ClazzData) => (
                <div
                  key={classItem.id}
                  className={
                    selectedClazzes.includes(classItem.id)
                      ? `${styles.clazzRow} ${styles.clazzRowSelected}`
                      : styles.clazzRow
                  }
                  onClick={() => handleClassClick(classItem.id)}
                  onContextMenu={(e) => handleRightClick(e, classItem.id)}
                >
                  <div>{classItem.name}</div>
                  <div>{(classItem.similarity * 100).toFixed(2)}%</div>
                </div>
              ))}

              {showContextMenu && (
                <div
                  style={{
                    position: 'fixed',
                    top: menuPosition.y,
                    left: menuPosition.x,
                    background: 'black',
                    boxShadow: '0 2px 10px rgba(0,0,0,0.3)',
                    borderRadius: '4px',
                    zIndex: 1000,
                  }}
                >
                  <div className={styles.contextMenuItem} onClick={handleSetRefClazzes}>
                    Set as Ref Classes
                  </div>
                  <div className={styles.contextMenuItem} onClick={handleSplit}>
                    Split
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        <div style={{ marginBottom: '20px' }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              cursor: 'pointer',
              padding: '5px 0',
              borderBottom: '1px solid #333',
              marginBottom: expandedSections.dependencies ? '10px' : '0',
            }}
            onClick={() => toggleSection('dependencies')}
          >
            <div style={{ fontWeight: 'bold' }}>Dependencies</div>
            <div>{expandedSections.dependencies ? '' : '+'}</div>
          </div>

          {expandedSections.dependencies && (
            <div>
              {module.allDependencies.map((dependency) => (
                <div
                  key={dependency.packageName}
                  style={{
                    padding: '8px 10px',
                    borderRadius: '4px',
                    marginBottom: '2px',
                    backgroundColor: 'black',
                  }}
                >
                  {dependency.packageName}
                </div>
              ))}
            </div>
          )}
        </div>

        <div>
          <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>Module Similarity:</div>
          <div>{(module.similarity * 100).toFixed(2)}%</div>
        </div>
      </div>
    </div>
  );
};

export default ModuleDetailView;

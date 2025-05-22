import { useEffect, useRef, useState } from 'react';
import { ConfirmationModalProps, ModuleCardProps, SidebarProps } from './sidebarTypes';
import { ModuleData } from '../../interface/ModuleData';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheck, faChevronLeft, faEllipsisVertical, faPencil, faTimes, faTrash } from '@fortawesome/free-solid-svg-icons';
import axios from 'axios';
import { ClazzData } from '../../interface/ClazzData.tsx';

// Confirmation Modal Component
const ConfirmationModal = ({ isOpen, title, message, onConfirm, onCancel }: ConfirmationModalProps) => {
    if (!isOpen) return null;

    return (
        <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 2000
        }}>
            <div style={{
                backgroundColor: '#333',
                borderRadius: '8px',
                padding: '20px',
                width: '300px',
                color: 'white'
            }}>
                <h3 style={{ margin: '0 0 10px 0' }}>{title}</h3>
                <p style={{ margin: '0 0 20px 0' }}>{message}</p>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                    <button
                        onClick={onCancel}
                        style={{
                            padding: '8px 16px',
                            border: 'none',
                            borderRadius: '4px',
                            backgroundColor: '#555',
                            color: 'white',
                            cursor: 'pointer'
                        }}
                    >
                        Cancel
                    </button>
                    <button
                        onClick={onConfirm}
                        style={{
                            padding: '8px 16px',
                            border: 'none',
                            borderRadius: '4px',
                            backgroundColor: '#ff4d4d',
                            color: 'white',
                            cursor: 'pointer'
                        }}
                    >
                        Delete
                    </button>
                </div>
            </div>
        </div>
    );
};

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
            style={{
                width: '100%',
                backgroundColor: 'black',
                color: 'white',
                padding: '12px',
                borderRadius: '6px',
                marginBottom: '10px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                cursor: isRenaming ? 'default' : 'pointer',
                transition: 'background-color 0.2s',
                ':hover': {
                    backgroundColor: '#333'
                },
                boxSizing: 'border-box'
            }}
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
                                width: '70%'
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
                                marginLeft: '5px'
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
                                marginLeft: '5px'
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
                        padding: '5px'
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
                            minWidth: '120px'
                        }}
                    >
                        <div
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsMenuOpen(false);
                                setIsRenaming(true);
                            }}
                            style={{
                                padding: '8px 12px',
                                display: 'flex',
                                alignItems: 'center',
                                cursor: 'pointer',
                                ':hover': {
                                    backgroundColor: '#333'
                                }
                            }}
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
                            style={{
                                padding: '8px 12px',
                                display: 'flex',
                                alignItems: 'center',
                                cursor: 'pointer',
                                ':hover': {
                                    backgroundColor: '#333'
                                }
                            }}
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

// Module List View
const ModuleListView = ({ modules, onModuleClick, onModuleRename, onModuleDelete }) => {
    return (
        <div style={{
            padding: '15px',
            height: '100%',
            overflowY: 'auto',
            boxSizing: 'border-box'
        }}>
            <h3 style={{ marginTop: 0, marginBottom: '15px' }}>Modules</h3>
            {modules?.map((module: ModuleData) => (
                <ModuleCard
                    key={module.id}
                    module={module}
                    onClick={onModuleClick}
                    onRename={onModuleRename}
                    onDelete={onModuleDelete}
                />
            ))}
        </div>
    );
};

// Module Detail View
const ModuleDetailView = ({ module, onBack, onSplit }) => {
    const [selectedClazzes, setSelectedClazzes] = useState<string[]>([]);
    const [showSplitMenu, setShowSplitMenu] = useState(false);
    const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });
    const [expandedSections, setExpandedSections] = useState({
        clazzes: true,
        dependencies: false
    });

    const handleClassClick = (classId: string) => {
        setSelectedClazzes(prev => {
            if (prev.includes(classId)) {
                return prev.filter(id => id !== classId);
            } else {
                return [...prev, classId];
            }
        });
    };

    const handleRightClick = (e: React.MouseEvent) => {
        if (selectedClazzes.length > 0) {
            e.preventDefault();
            setMenuPosition({ x: e.clientX, y: e.clientY });
            setShowSplitMenu(true);
        }
    };

    const handleSplit = async () => {
        try {
            await onSplit(module.id, selectedClazzes);
            setSelectedClazzes([]);
            setShowSplitMenu(false);
        } catch (error) {
            console.error('Failed to split module', error);
        }
    };

    useEffect(() => {
        const handleClickOutside = () => {
            setShowSplitMenu(false);
        };

        document.addEventListener('click', handleClickOutside);
        return () => {
            document.removeEventListener('click', handleClickOutside);
        };
    }, []);

    const toggleSection = (section: string) => {
        setExpandedSections(prev => ({
            ...prev,
            [section]: !prev[section]
        }));
    };

    return (
        <div style={{
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            color: 'white'
        }}>
            <div style={{
                padding: '15px',
                display: 'flex',
                alignItems: 'center',
                borderBottom: '1px solid #333'
            }}>
                <FontAwesomeIcon
                    icon={faChevronLeft}
                    onClick={onBack}
                    style={{
                        cursor: 'pointer',
                        marginRight: '10px',
                        fontSize: '14px'
                    }}
                />
                <h3 style={{ margin: 0 }}>{module.name}</h3>
            </div>

            <div style={{
                flex: 1,
                overflowY: 'auto',
                padding: '15px'
            }}>
                <div style={{ marginBottom: '20px' }}>
                    <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>Reference Class:</div>
                    <div style={{ color: '#4CAF50' }}>{module.refClass}</div>
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
                            marginBottom: expandedSections.clazzes ? '10px' : '0'
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
                                borderRadius: '4px'
                            }}
                            onContextMenu={handleRightClick}
                        >
                            {module.clazzes.map((classItem: ClazzData) => (
                                <div
                                    key={classItem.id}
                                    style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        padding: '8px 10px',
                                        backgroundColor: selectedClazzes.includes(classItem.id) ? '#333' : 'transparent',
                                        cursor: 'pointer',
                                        ':hover': {
                                            backgroundColor: '#222'
                                        },
                                        borderRadius: '4px',
                                        marginBottom: '2px'
                                    }}
                                    onClick={() => handleClassClick(classItem.id)}
                                >
                                    <div>{classItem.name}</div>
                                    <div>{classItem.similarity.toFixed(2)}</div>
                                </div>
                            ))}

                            {showSplitMenu && (
                                <div style={{
                                    position: 'fixed',
                                    top: menuPosition.y,
                                    left: menuPosition.x,
                                    background: 'black',
                                    boxShadow: '0 2px 10px rgba(0,0,0,0.3)',
                                    borderRadius: '4px',
                                    zIndex: 1000
                                }}>
                                    <div
                                        style={{
                                            padding: '8px 12px',
                                            cursor: 'pointer',
                                            ':hover': {
                                                backgroundColor: '#333'
                                            }
                                        }}
                                        onClick={handleSplit}
                                    >
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
                            marginBottom: expandedSections.dependencies ? '10px' : '0'
                        }}
                        onClick={() => toggleSection('dependencies')}
                    >
                        <div style={{ fontWeight: 'bold' }}>Dependencies</div>
                        <div>{expandedSections.dependencies ? '' : '+'}</div>
                    </div>

                    {expandedSections.dependencies && (
                        <div>
                            {module.types.map((type, index) => (
                                <div
                                    key={index}
                                    style={{
                                        padding: '8px 10px',
                                        borderRadius: '4px',
                                        marginBottom: '2px',
                                        backgroundColor: 'black'
                                    }}
                                >
                                    {type.name}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div>
                    <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>Module Similarity:</div>
                    <div>{module.similarity.toFixed(2)}</div>
                </div>
            </div>
        </div>
    );
};

// Main Sidebar Component
const Sidebar = ({ isOpen, modules, onRequestRefresh }: SidebarProps) => {
    const [activeView, setActiveView] = useState<'list' | 'detail'>('list');
    const [selectedModule, setSelectedModule] = useState<ModuleData | null>(null);
    const [deleteConfirmation, setDeleteConfirmation] = useState({
        isOpen: false,
        moduleId: null
    });

    const handleModuleClick = (module: ModuleData) => {
        setSelectedModule(module);
        setActiveView('detail');
    };

    const handleBackToList = () => {
        setActiveView('list');
        setSelectedModule(null);
    };

    const handleModuleRename = async (moduleId: number, newName: string) => {
        try {
            await axios.post('/api/module/rename', null, {
                params: {
                    moduleId,
                    newName
                }
            });
            onRequestRefresh();
        } catch (error) {
            console.error('Failed to rename module', error);
            throw error;
        }
    };

    const handleModuleDelete = async (moduleId: number) => {
        setDeleteConfirmation({
            isOpen: true,
            moduleId
        });
    };

    const confirmDelete = async () => {
        if (!deleteConfirmation.moduleId) return;

        try {
            await axios.delete('/api/module/delete', {
                params: {
                    moduleId: deleteConfirmation.moduleId
                }
            });
            setDeleteConfirmation({
                isOpen: false,
                moduleId: null
            });
            onRequestRefresh();

            // If we're in detail view and deleted the current module, go back to list
            if (activeView === 'detail' && selectedModule?.id === deleteConfirmation.moduleId) {
                handleBackToList();
            }
        } catch (error) {
            console.error('Failed to delete module', error);
        }
    };

    const cancelDelete = () => {
        setDeleteConfirmation({
            isOpen: false,
            moduleId: null
        });
    };

    const handleSplitModule = async (moduleId: number, classIds: number[]) => {
        try {
            await axios.post('/api/module/split', null, {
                params: {
                    moduleId,
                    classIds: JSON.stringify(classIds)
                }
            });
            onRequestRefresh();
            handleBackToList(); // Return to list view after split
        } catch (error) {
            console.error('Failed to split module', error);
            throw error;
        }
    };

    return (
        <>
            <div style={{
                position: 'fixed',
                top: '60px', // Height of header
                left: 0,
                width: isOpen ? '300px' : '0',
                height: 'calc(100vh - 60px)', // Full height minus header
                backgroundColor: 'black',
                color: 'white',
                boxShadow: isOpen ? '2px 0 10px rgba(0,0,0,0.2)' : 'none',
                transition: 'width 0.3s ease',
                overflow: 'hidden',
                zIndex: 100
            }}>
                {activeView === 'list' ? (
                    <ModuleListView
                        modules={modules}
                        onModuleClick={handleModuleClick}
                        onModuleRename={handleModuleRename}
                        onModuleDelete={handleModuleDelete}
                    />
                ) : (
                    selectedModule && (
                        <ModuleDetailView
                            module={selectedModule}
                            onBack={handleBackToList}
                            onSplit={handleSplitModule}
                        />
                    )
                )}
            </div>

            <ConfirmationModal
                isOpen={deleteConfirmation.isOpen}
                title="Delete Module"
                message="Are you sure you want to delete this module? This action cannot be undone."
                onConfirm={confirmDelete}
                onCancel={cancelDelete}
            />
        </>
    );
};

export default Sidebar;
import { useState } from 'react';
import axios from 'axios';
import { SidebarProps } from './sidebarTypes';
import { ModuleData, SplitModuleResponse } from '../../interface/ModuleData';
import { EnforceMode } from '../../interface/ClazzData.tsx';
import ConfirmationModal from './ConfirmationModal';
import ModuleListView from './ModuleListView';
import ModuleDetailView from './ModuleDetailView';

// Main Sidebar Component
const Sidebar = ({
  isOpen,
  modules,
  onDeleteRefresh,
  onRenameRefresh,
  onSplitRefresh,
  onRefClazzModeRefresh,
}: SidebarProps) => {
  const [activeView, setActiveView] = useState<'list' | 'detail'>('list');
  const [selectedModule, setSelectedModule] = useState<ModuleData | null>(null);
  const [deleteConfirmation, setDeleteConfirmation] = useState<{
    isOpen: boolean;
    moduleId: string | null;
  }>({
    isOpen: false,
    moduleId: null,
  });

  const handleModuleClick = (module: ModuleData) => {
    setSelectedModule(module);
    setActiveView('detail');
  };

  const handleBackToList = () => {
    setActiveView('list');
    setSelectedModule(null);
  };

  const handleModuleRename = async (moduleId: string, newName: string) => {
    try {
      await axios.post('/api/module/rename', null, {
        params: {
          moduleId,
          newName,
        },
      });
      onRenameRefresh(moduleId, newName);
    } catch (error) {
      console.error('Failed to rename module', error);
      throw error;
    }
  };

  const handleModuleDelete = async (moduleId: string) => {
    setDeleteConfirmation({
      isOpen: true,
      moduleId,
    });
  };

  const confirmDelete = async () => {
    if (!deleteConfirmation.moduleId) return;

    try {
      await axios.delete('/api/module/delete', {
        params: {
          moduleId: deleteConfirmation.moduleId,
        },
      });
      setDeleteConfirmation({
        isOpen: false,
        moduleId: null,
      });
      onDeleteRefresh(deleteConfirmation.moduleId);

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
      moduleId: null,
    });
  };

  const handleSplitModule = async (moduleId: string, classIds: string[]) => {
    try {
      const response = await axios.post<SplitModuleResponse>('/api/module/split', {
        moduleId,
        classIds,
      });

      // Pass the modules on whole: the graph nodes read their reference classes.
      onSplitRefresh(moduleId, response.data.newModules);
      handleBackToList();
    } catch (error) {
      console.error('Failed to split module', error);
      throw error;
    }
  };

  const handleSetRefClazzes = async (moduleId: string, classIds: string[]) => {
    try {
      const response = await axios.post<ModuleData>('/api/module/ref-clazzes', {
        moduleId,
        classIds,
      });

      // Update the selected module with the new reference classes
      if (selectedModule && selectedModule.id === moduleId) {
        const updatedModule = {
          ...selectedModule,
          refClazzes: response.data.refClazzes,
        };
        setSelectedModule(updatedModule);
      }
    } catch (error) {
      console.error('Failed to set reference classes', error);
      throw error;
    }
  };

  const handleSetRefClazzMode = async (moduleId: string, classId: string, mode: EnforceMode) => {
    try {
      const response = await axios.post<ModuleData>('/api/module/ref-clazz-mode', {
        moduleId,
        classId,
        mode,
      });

      if (selectedModule && selectedModule.id === moduleId) {
        const updatedModule = {
          ...selectedModule,
          refClazzes: response.data.refClazzes,
          clazzes: response.data.clazzes,
        };
        setSelectedModule(updatedModule);
        onRefClazzModeRefresh(moduleId, updatedModule);
      }
    } catch (error) {
      console.error('Failed to set enforce mode', error);
    }
  };

  return (
    <>
      <div
        style={{
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
          zIndex: 100,
        }}
      >
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
              onSetRefClazzes={handleSetRefClazzes}
              onSetRefClazzMode={handleSetRefClazzMode}
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

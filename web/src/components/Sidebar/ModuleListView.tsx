import { ModuleListViewProps } from './sidebarTypes';
import ModuleCard from './ModuleCard';

// Module List View
const ModuleListView = ({
  modules,
  onModuleClick,
  onModuleRename,
  onModuleDelete,
}: ModuleListViewProps) => {
  return (
    <div
      style={{
        padding: '15px',
        height: '100%',
        overflowY: 'auto',
        boxSizing: 'border-box',
      }}
    >
      <h3 style={{ marginTop: 0, marginBottom: '15px' }}>Modules</h3>
      {modules.map((module) => (
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

export default ModuleListView;

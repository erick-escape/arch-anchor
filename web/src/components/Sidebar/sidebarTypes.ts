import { ModuleData } from '../../interface/ModuleData';

export interface SidebarProps {
  isOpen: boolean;
  modules: ModuleData[];
  onDeleteRefresh: (deletedModuleId: string) => void;
  onRenameRefresh: (moduleId: string, newName: string) => void;
  onSplitRefresh: (oldModuleId: string, newModules: ModuleData[]) => void;
}

export interface ModuleListViewProps {
  modules: ModuleData[];
  onModuleClick: (module: ModuleData) => void;
  onModuleRename: (moduleId: number, newName: string) => Promise<void>;
  onModuleDelete: (moduleId: number) => Promise<void>;
}

export interface ModuleDetailViewProps {
  module: ModuleData;
  onBack: () => void;
  onSplit: (moduleId: string, classIds: string[]) => Promise<void>;
}

export interface ModuleCardProps {
  module: ModuleData;
  onClick: (module: ModuleData) => void;
  onRename: (moduleId: string, newName: string) => Promise<void>;
  onDelete: (moduleId: string) => Promise<void>;
}

export interface ConfirmationModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export interface CardMenuItemProps {
  icon: JSX.Element;
  label: string;
  onClick: () => void;
  color?: string;
}

export interface HeaderProps {
  projectName: string;
  isSidebarOpen: boolean;
  onToggleSidebar: () => void;
  onExportACs: () => void;
  onOpenSettings: () => void;
}

export interface HeaderMenuOption {
  label: string;
  onClick: () => void;
}

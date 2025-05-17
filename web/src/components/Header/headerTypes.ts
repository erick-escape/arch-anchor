export interface HeaderProps {
    projectName: string;
    isSidebarOpen: boolean;
    onToggleSidebar: () => void;
}

export interface HeaderMenuOption {
    label: string;
    onClick: () => void;
}
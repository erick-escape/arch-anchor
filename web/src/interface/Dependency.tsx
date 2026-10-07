// Mirrors DependencyDTO / DependencyDTO.TypeDTO in api/.

export interface DependencyType {
  fullyQualifiedName: string;
  className: string;
}

export interface Dependency {
  packageName: string;
  types: DependencyType[];
}

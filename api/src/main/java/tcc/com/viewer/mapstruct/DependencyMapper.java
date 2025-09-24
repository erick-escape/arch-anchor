package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.dependency.Type;
import tcc.com.viewer.dto.dependencies.DependencyDTO;

@Mapper(componentModel = "spring")
public interface DependencyMapper {

    // Convert from DTO to entity
    Dependency toEntity(DependencyDTO dto);

    // Convert from entity to DTO (if needed)
    DependencyDTO toDto(Dependency entity);

    // Convert Type to TypeDTO
    @Mapping(source = "fullyQualifiedName", target = "fullyQualifiedName")
    @Mapping(source = "className", target = "className")
    DependencyDTO.TypeDTO toTypeDto(Type type);

    // Convert TypeDTO to Type
    @Mapping(source = "fullyQualifiedName", target = "fullyQualifiedName")
    @Mapping(source = "className", target = "className")
    Type toTypeEntity(DependencyDTO.TypeDTO typeDto);
}
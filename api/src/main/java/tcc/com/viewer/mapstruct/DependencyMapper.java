package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.dto.dependencies.DependencyDTO;

@Mapper(componentModel = "spring")
public interface DependencyMapper {

    // Convert from DTO to entity
    Dependency toEntity(DependencyDTO dto);

    // Convert from entity to DTO (if needed)
    DependencyDTO toDto(Dependency entity);
}
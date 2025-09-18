package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import tcc.com.viewer.domains.dependency.DependencyOrigin;
import tcc.com.viewer.dto.dependencies.DependencyOriginDTO;

@Mapper(componentModel = "spring")
public interface DependencyOriginMapper {

    // Convert from DTO to entity
    DependencyOrigin toEntity(DependencyOriginDTO dto);

    // Convert from entity to DTO (if needed)
    DependencyOriginDTO toDto(DependencyOrigin entity);
}
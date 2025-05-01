package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.module.ModuleDTO;

@Mapper(componentModel = "spring")
public interface ModuleMapper {
    // Convert from DTO to entity
    Module toEntity(ModuleDTO dto);

    // Convert from entity to DTO (if needed)
    ModuleDTO toDto(Module entity);
}

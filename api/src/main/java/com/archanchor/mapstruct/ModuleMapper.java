package com.archanchor.mapstruct;

import org.mapstruct.Mapper;
import com.archanchor.domains.module.Module;
import com.archanchor.dto.module.ModuleDTO;

@Mapper(componentModel = "spring")
public interface ModuleMapper {

	Module toEntity(ModuleDTO dto);

	ModuleDTO toDto(Module entity);

}

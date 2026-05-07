package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.module.ModuleDTO;

@Mapper(componentModel = "spring")
public interface ModuleMapper {

	Module toEntity(ModuleDTO dto);

	ModuleDTO toDto(Module entity);

}

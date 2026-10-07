package com.archanchor.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.archanchor.domains.module.Module;
import com.archanchor.dto.module.ModuleDTO;

// ERROR, not the default WARN: an unmapped field here silently nulled
// ModuleDTO.allDependencies in every API response (#8).
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ModuleMapper {

	@Mapping(source = "allDependencies", target = "moduleDependencies")
	// ModuleDTO does not carry violations; callers recompute them with
	// ModuleService.calculateModuleViolations when they need them.
	@Mapping(target = "violations", ignore = true)
	Module toEntity(ModuleDTO dto);

	@Mapping(source = "moduleDependencies", target = "allDependencies")
	ModuleDTO toDto(Module entity);

}

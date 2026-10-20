package com.archanchor.mapstruct;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

// ERROR, not the default WARN: an unmapped field in ModuleMapper silently nulled
// ModuleDTO.allDependencies in every API response (#8). Every mapper uses this config, so
// a field added or renamed on one side only fails the build instead (#10).
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StrictMapperConfig {

}

package com.archanchor.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.dto.dependencies.DependencyDTO;

@Mapper(componentModel = "spring")
public interface DependencyMapper {

	Dependency toEntity(DependencyDTO dto);

	DependencyDTO toDto(Dependency entity);

	@Mapping(source = "fullyQualifiedName", target = "fullyQualifiedName")
	@Mapping(source = "className", target = "className")
	DependencyDTO.TypeDTO toTypeDto(Type type);

	@Mapping(source = "fullyQualifiedName", target = "fullyQualifiedName")
	@Mapping(source = "className", target = "className")
	Type toTypeEntity(DependencyDTO.TypeDTO typeDto);

}
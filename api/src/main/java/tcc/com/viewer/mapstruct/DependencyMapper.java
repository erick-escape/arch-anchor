package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.dependency.Type;
import tcc.com.viewer.dto.dependencies.DependencyDTO;

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
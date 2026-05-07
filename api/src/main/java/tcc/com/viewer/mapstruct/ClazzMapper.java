package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.dto.clazz.ClazzResponseDTO;

@Mapper(componentModel = "spring")
public interface ClazzMapper {

	Clazz toEntity(ClazzResponseDTO dto);

	ClazzResponseDTO toDto(Clazz entity);

}
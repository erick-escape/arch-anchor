package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.dto.clazz.ClazzResponseDTO;

@Mapper(componentModel = "spring")
public interface ClazzMapper {
    // Convert from DTO to entity
    Clazz toEntity(ClazzResponseDTO dto);

    // Convert from entity to DTO (if needed)
    ClazzResponseDTO toDto(Clazz entity);
}
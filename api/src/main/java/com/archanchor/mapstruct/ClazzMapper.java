package com.archanchor.mapstruct;

import org.mapstruct.Mapper;
import com.archanchor.domains.clazz.Clazz;
import com.archanchor.dto.clazz.ClazzResponseDTO;

@Mapper(config = StrictMapperConfig.class)
public interface ClazzMapper {

	Clazz toEntity(ClazzResponseDTO dto);

	ClazzResponseDTO toDto(Clazz entity);

}
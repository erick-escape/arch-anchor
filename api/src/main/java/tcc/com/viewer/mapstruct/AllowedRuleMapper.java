package tcc.com.viewer.mapstruct;

import org.mapstruct.Mapper;
import tcc.com.viewer.domains.rules.AllowedRule;
import tcc.com.viewer.dto.rules.AllowedRuleDTO;

@Mapper(componentModel = "spring")
public interface AllowedRuleMapper {
    // Convert from DTO to entity
    AllowedRule toEntity(AllowedRuleDTO dto);

    // Convert from entity to DTO (if needed)
    AllowedRuleDTO toDto(AllowedRule entity);
}

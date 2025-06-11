package tcc.com.viewer.dto.module;

import java.util.List;

public record SplitModuleResponse(
        List<ModuleDTO> newModules
) {
}
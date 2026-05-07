package tcc.com.viewer.dto.module;

import java.util.List;

public record SplitModuleRequest(String moduleId, List<String> classIds) {
}
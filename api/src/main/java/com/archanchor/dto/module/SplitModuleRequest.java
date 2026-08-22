package com.archanchor.dto.module;

import java.util.List;

public record SplitModuleRequest(String moduleId, List<String> classIds) {
}
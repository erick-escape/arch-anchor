package com.archanchor.dto.module;

import java.util.List;

public record SetRefClazzesRequest(String moduleId, List<String> classIds) {
}
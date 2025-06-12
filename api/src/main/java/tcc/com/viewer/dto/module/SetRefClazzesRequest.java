package tcc.com.viewer.dto.module;

import java.util.List;

public record SetRefClazzesRequest(
    String moduleId,
    List<String> classIds
) {
}
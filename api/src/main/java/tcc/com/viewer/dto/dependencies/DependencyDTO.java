package tcc.com.viewer.dto.dependencies;

import java.io.Serializable;

public record DependencyDTO(String fullyQualifiedName, String originName) implements Serializable {
}

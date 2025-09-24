package tcc.com.viewer.dto.dependencies;

import java.io.Serializable;
import java.util.List;

public record DependencyDTO(String packageName, List<TypeDTO> types) implements Serializable {

    public record TypeDTO(String fullyQualifiedName, String className) implements Serializable {
    }
}

package tcc.com.viewer.domains.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Dependency {
    private String fullyQualifiedName;
    private String originName;

    public boolean dependencyDoesNotExist(List<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            if (dependency.getFullyQualifiedName().contains(this.fullyQualifiedName)) {
                return false;
            }
        }
        return true;
    }
}

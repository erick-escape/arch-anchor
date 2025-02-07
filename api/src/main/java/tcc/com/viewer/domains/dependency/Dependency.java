package tcc.com.viewer.domains.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Dependency {
    private String name;

    public boolean dependencyDoesNotExist(List<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            if (dependency.getName().contains(this.name)) {
                return false;
            }
        }
        return true;
    }
}

package tcc.com.viewer.domains.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import tcc.com.viewer.antlr4.JavaParser;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Dependency {
    private String name;
    private JavaParser.TypeTypeContext typeType;

    public boolean dependencyDoesNotExist(List<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            if (dependency.getName().equals(this.name)) {
                return false;
            }
        }
        return true;
    }
}

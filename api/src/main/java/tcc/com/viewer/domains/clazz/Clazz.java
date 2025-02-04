package tcc.com.viewer.domains.clazz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import tcc.com.viewer.domains.dependency.Dependency;

@Getter
@Setter
@AllArgsConstructor
public class Clazz {
    private String name;
    private Dependency[] dependencies;
    private Double similarity;
    private final String firstModule;
    private String currentModule;
}

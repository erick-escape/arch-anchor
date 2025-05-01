package tcc.com.viewer.domains.module;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;

@Getter
@Setter
@AllArgsConstructor
public class Module {
    private String id;
    private String name;
    private String refClass;
    private Clazz[] clazzes;
    private Dependency[] dependencies;
    private Double similarity;
}
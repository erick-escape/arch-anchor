package tcc.com.viewer.domains.module;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.dependency.DependencyOrigin;
import tcc.com.viewer.domains.rules.AllowedRule;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Module {
    private String id;
    private String name;
    private List<Clazz> refClazzes;
    private List<AllowedRule> allowedRules;
    private List<DependencyOrigin> allDependenciesOrigin;
    private List<Dependency> refClazzesDependencies;
    private List<Dependency> allDependencies;
    private List<Clazz> clazzes;
    private Double similarity;
}
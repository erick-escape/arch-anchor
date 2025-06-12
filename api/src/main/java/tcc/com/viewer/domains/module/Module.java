package tcc.com.viewer.domains.module;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Module {
    private String id;
    private String name;
    private List<Clazz> refClazzes;
    private Clazz[] clazzes;
    private Dependency[] dependencies;
    private Double similarity;
}
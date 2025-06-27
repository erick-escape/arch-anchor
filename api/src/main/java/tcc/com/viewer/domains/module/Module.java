package tcc.com.viewer.domains.module;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tcc.com.viewer.domains.clazz.Clazz;
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
    private List<Clazz> clazzes;
    private Double similarity;
}
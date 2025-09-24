package tcc.com.viewer.domains.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Dependency {
    private String packageName;
    private List<Type> types;

    public Dependency(String packageName) {
        this.packageName = packageName;
        this.types = new ArrayList<>();
    }

    public void addType(Type type) {
        if (types == null) {
            types = new ArrayList<>();
        }
        types.add(type);
    }
}

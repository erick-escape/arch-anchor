package com.archanchor.mapstruct;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.domains.module.Module;
import com.archanchor.dto.dependencies.DependencyDTO;
import com.archanchor.dto.module.ModuleDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModuleMapper}. Regression suite for issue #8: the domain field
 * {@code moduleDependencies} and the DTO component {@code allDependencies} have different
 * names, so MapStruct silently left both null.
 */
class ModuleMapperTest {

	private final ModuleMapper moduleMapper = new ModuleMapperImpl();

	@Test
	void toDtoCopiesModuleDependenciesIntoAllDependencies() {
		ModuleDTO dto = moduleMapper.toDto(moduleWithDependencies(springAndJavaUtil()));

		assertThat(dto.allDependencies()).extracting(DependencyDTO::packageName)
			.containsExactly("org.springframework.stereotype", "java.util");
		assertThat(dto.allDependencies()[1].types()).extracting(DependencyDTO.TypeDTO::fullyQualifiedName)
			.containsExactly("java.util.List", "java.util.Map");
	}

	@Test
	void toEntityCopiesAllDependenciesIntoModuleDependencies() {
		ModuleDTO dto = moduleMapper.toDto(moduleWithDependencies(springAndJavaUtil()));

		Module entity = moduleMapper.toEntity(dto);

		assertThat(entity.getModuleDependencies()).extracting(Dependency::getPackageName)
			.containsExactly("org.springframework.stereotype", "java.util");
	}

	@Test
	void roundTripKeepsDependenciesAndTheirTypes() {
		// Rename and enforce-mode changes convert entity -> DTO -> entity without
		// recomputing dependencies, so the round trip itself must not lose them.
		Module original = moduleWithDependencies(springAndJavaUtil());

		Module roundTripped = moduleMapper.toEntity(moduleMapper.toDto(original));

		assertThat(roundTripped.getModuleDependencies()).usingRecursiveFieldByFieldElementComparator()
			.containsExactlyElementsOf(original.getModuleDependencies());
	}

	@Test
	void moduleWithoutDependenciesMapsToAnEmptyArray() {
		ModuleDTO dto = moduleMapper.toDto(moduleWithDependencies(List.of()));

		assertThat(dto.allDependencies()).isEmpty();
	}

	private static List<Dependency> springAndJavaUtil() {
		return List.of(
				new Dependency("org.springframework.stereotype",
						List.of(new Type("org.springframework.stereotype.Service"))),
				new Dependency("java.util", List.of(new Type("java.util.List"), new Type("java.util.Map"))));
	}

	private static Module moduleWithDependencies(List<Dependency> moduleDependencies) {
		return new Module("m1", "orders", List.of(), List.of(), moduleDependencies, List.of(), 0.5, 1.0, 0);
	}

}

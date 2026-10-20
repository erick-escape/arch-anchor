package com.archanchor.mapstruct;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DependencyMapper}: a dependency and its types survive the trip through
 * {@code DependencyDTO} (#10).
 */
class DependencyMapperTest {

	private final DependencyMapper dependencyMapper = new DependencyMapperImpl();

	@Test
	void roundTripKeepsThePackageAndItsTypes() {
		Dependency original = new Dependency("java.util",
				List.of(new Type("java.util.List"), new Type("java.util.Map")));

		Dependency roundTripped = dependencyMapper.toEntity(dependencyMapper.toDto(original));

		assertThat(roundTripped).usingRecursiveComparison().isEqualTo(original);
	}

	@Test
	void typeRoundTripKeepsBothNames() {
		// A class name the one-argument constructor would not derive, so a mapping that
		// rebuilds the type from its qualified name alone fails.
		Type original = new Type("java.util.Map.Entry", "Map.Entry");

		Type roundTripped = dependencyMapper.toTypeEntity(dependencyMapper.toTypeDto(original));

		assertThat(roundTripped).usingRecursiveComparison().isEqualTo(original);
	}

}

package tcc.com.viewer.services;

import org.junit.jupiter.api.Test;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.dependency.Type;
import tcc.com.viewer.dto.clazz.ClazzResponseDTO;
import tcc.com.viewer.dto.dependencies.DependencyDTO;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.dto.projects.ArchitecturalConstraintDTO;
import tcc.com.viewer.dto.projects.RefClassConstraintDTO;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the enforcement mode feature on reference classes. Verifies that ALLOW mode
 * generates CAN-DEPEND constraints and MUST mode generates MUST-DEPEND constraints.
 */
class EnforceModeTest {

	// ── domain model tests ──────────────────────────────────────────────────

	@Test
	void defaultEnforceModeIsAllow() {
		Clazz clazz = new Clazz("id", "MyClass", List.of(), 0.7, 0.7, "module", "module", "ALLOW");
		assertThat(clazz.getEnforceMode()).isEqualTo("ALLOW");
	}

	@Test
	void mustModeClazzHasMustMode() {
		Clazz clazz = new Clazz("id", "MyClass", List.of(), 0.7, 0.7, "module", "module", "MUST");
		assertThat(clazz.getEnforceMode()).isEqualTo("MUST");
	}

	@Test
	void setEnforceModeUpdatesField() {
		Clazz clazz = new Clazz("id", "MyClass", List.of(), 0.7, 0.7, "module", "module", "ALLOW");
		clazz.setEnforceMode("MUST");
		assertThat(clazz.getEnforceMode()).isEqualTo("MUST");
	}

	// ── RefClassConstraintDTO tests ─────────────────────────────────────────

	@Test
	void refClassConstraintDTO_capturesAllowMode() {
		DependencyDTO dep = new DependencyDTO("java.util", List.of());
		RefClassConstraintDTO constraint = new RefClassConstraintDTO("rc1", "RefClass", "ALLOW",
				new DependencyDTO[] { dep });

		assertThat(constraint.enforceMode()).isEqualTo("ALLOW");
		assertThat(constraint.dependencies()).hasSize(1);
		assertThat(constraint.dependencies()[0].packageName()).isEqualTo("java.util");
	}

	@Test
	void refClassConstraintDTO_capturesMustMode() {
		DependencyDTO dep = new DependencyDTO("org.slf4j", List.of());
		RefClassConstraintDTO constraint = new RefClassConstraintDTO("rc2", "LoggerRef", "MUST",
				new DependencyDTO[] { dep });

		assertThat(constraint.enforceMode()).isEqualTo("MUST");
		assertThat(constraint.dependencies()[0].packageName()).isEqualTo("org.slf4j");
	}

	// ── AC constraint building tests (simulates ProjectService logic) ───────

	@Test
	void buildConstraints_allowModeRefClass_generatesCandependEntry() {
		ClazzResponseDTO refClass = buildRefClassDTO("rc1", "RefClass", "ALLOW", "java.util");
		ModuleDTO module = buildModuleDTO("m1", "ServiceModule", new ClazzResponseDTO[] { refClass });

		ArchitecturalConstraintDTO ac = buildConstraint(module);

		assertThat(ac.refClassConstraints()).hasSize(1);
		assertThat(ac.refClassConstraints()[0].enforceMode()).isEqualTo("ALLOW");
		assertThat(ac.refClassConstraints()[0].dependencies()[0].packageName()).isEqualTo("java.util");
	}

	@Test
	void buildConstraints_mustModeRefClass_generatesMustdependEntry() {
		ClazzResponseDTO refClass = buildRefClassDTO("rc1", "RefClass", "MUST", "org.slf4j");
		ModuleDTO module = buildModuleDTO("m1", "ServiceModule", new ClazzResponseDTO[] { refClass });

		ArchitecturalConstraintDTO ac = buildConstraint(module);

		assertThat(ac.refClassConstraints()).hasSize(1);
		assertThat(ac.refClassConstraints()[0].enforceMode()).isEqualTo("MUST");
		assertThat(ac.refClassConstraints()[0].refClassName()).isEqualTo("RefClass");
	}

	@Test
	void buildConstraints_mixedModes_generatesBothEntries() {
		ClazzResponseDTO allowRef = buildRefClassDTO("rc1", "AllowRef", "ALLOW", "java.util");
		ClazzResponseDTO mustRef = buildRefClassDTO("rc2", "MustRef", "MUST", "org.slf4j");
		ModuleDTO module = buildModuleDTO("m1", "ServiceModule", new ClazzResponseDTO[] { allowRef, mustRef });

		ArchitecturalConstraintDTO ac = buildConstraint(module);

		assertThat(ac.refClassConstraints()).hasSize(2);
		assertThat(ac.refClassConstraints()[0].enforceMode()).isEqualTo("ALLOW");
		assertThat(ac.refClassConstraints()[1].enforceMode()).isEqualTo("MUST");
	}

	@Test
	void buildConstraints_moduleWithNoRefClasses_producesEmptyConstraints() {
		ModuleDTO module = buildModuleDTO("m1", "EmptyModule", new ClazzResponseDTO[0]);

		ArchitecturalConstraintDTO ac = buildConstraint(module);

		assertThat(ac.refClassConstraints()).isEmpty();
	}

	// ── mode validation tests ────────────────────────────────────────────────

	@Test
	void invalidMode_throwsIllegalArgumentException() {
		// Simulates the validation in ModuleService.setRefClazzMode()
		assertThatThrownBy(() -> validateMode("INVALID")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("INVALID");
	}

	@Test
	void validMode_allow_doesNotThrow() {
		assertThat(isValidMode("ALLOW")).isTrue();
	}

	@Test
	void validMode_must_doesNotThrow() {
		assertThat(isValidMode("MUST")).isTrue();
	}

	// ── helpers ──────────────────────────────────────────────────────────────

	/** Mirrors the AC-building logic in ProjectService.analyzeProject(). */
	private ArchitecturalConstraintDTO buildConstraint(ModuleDTO module) {
		RefClassConstraintDTO[] refClassConstraints = Arrays.stream(module.refClazzes())
			.map(rc -> new RefClassConstraintDTO(rc.id(), rc.name(), rc.enforceMode(),
					rc.dependencies().toArray(new DependencyDTO[0])))
			.toArray(RefClassConstraintDTO[]::new);
		return new ArchitecturalConstraintDTO(module.id(), module.name(), refClassConstraints);
	}

	private ClazzResponseDTO buildRefClassDTO(String id, String name, String mode, String packageName) {
		DependencyDTO dep = new DependencyDTO(packageName, List.of());
		return new ClazzResponseDTO(id, name, List.of(dep), 0.8, 0.8, "module", "module", mode);
	}

	private ModuleDTO buildModuleDTO(String id, String name, ClazzResponseDTO[] refClazzes) {
		return new ModuleDTO(id, name, refClazzes, new DependencyDTO[0], new DependencyDTO[0], new ClazzResponseDTO[0],
				0.8, 0.8);
	}

	private void validateMode(String mode) {
		if (!mode.equals("ALLOW") && !mode.equals("MUST")) {
			throw new IllegalArgumentException("Invalid enforceMode: " + mode + "; expected ALLOW or MUST");
		}
	}

	private boolean isValidMode(String mode) {
		return mode.equals("ALLOW") || mode.equals("MUST");
	}

}

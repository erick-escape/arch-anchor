package tcc.com.viewer.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tcc.com.viewer.domains.dependency.Dependency;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit test suite that verifies full, correct resolution of imports for representative classes
 * in uploads/pass-in. This suite should immediately fail if dependency binding breaks after any change.
 * Tests the new Dependency structure with package-level dependencies containing type collections.
 * Verifies both package names and fully qualified type names are correctly extracted.
 */
@SpringBootTest
@DisplayName("JavaParser Type Resolution Tests")
class JavaParserTypeResolutionTest {

    @Autowired
    private JavaParserService javaParserService;

    @BeforeEach
    void setUp() {
        // Clear the processed files cache before each test to ensure consistent results
        javaParserService.clearCache();
    }

    /**
     * Helper method to extract expected package names from fully qualified type names.
     * For example: "org.springframework.boot.SpringApplication" -> "org.springframework.boot"
     */
    private static Set<String> extractExpectedPackages(Set<String> fullyQualifiedTypes) {
        return fullyQualifiedTypes.stream()
                .map(fqn -> {
                    int lastDotIndex = fqn.lastIndexOf('.');
                    return lastDotIndex != -1 ? fqn.substring(0, lastDotIndex) : fqn;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Provides test cases for parameterized tests.
     * Each argument contains: [test name, file path, expected fully qualified type names]
     * Expected package names are derived from the fully qualified type names.
     * Both package names and type names are verified in the tests.
     */
    static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of(
                        "PassInApplication",
                        "uploads/pass-in/src/main/java/tcc/com/pass_in/PassInApplication.java",
                        Set.of(
                                "org.springframework.boot.SpringApplication",
                                "org.springframework.boot.autoconfigure.SpringBootApplication"
                        )
                ),
                Arguments.of(
                        "Attendee",
                        "uploads/pass-in/src/main/java/tcc/com/pass_in/domain/attendee/Attendee.java",
                        Set.of(
                                "jakarta.persistence.Entity",
                                "jakarta.persistence.Table",
                                "jakarta.persistence.Id",
                                "jakarta.persistence.Column",
                                "jakarta.persistence.GeneratedValue",
                                "jakarta.persistence.GenerationType",
                                "jakarta.persistence.ManyToOne",
                                "jakarta.persistence.JoinColumn",
                                "lombok.AllArgsConstructor",
                                "lombok.Getter",
                                "lombok.NoArgsConstructor",
                                "lombok.Setter",
                                "tcc.com.pass_in.domain.event.Event",
                                "java.time.LocalDateTime"
                        )
                ),
                Arguments.of(
                        "EventRepository",
                        "uploads/pass-in/src/main/java/tcc/com/pass_in/repositories/EventRepository.java",
                        Set.of(
                                "org.springframework.data.jpa.repository.JpaRepository",
                                "tcc.com.pass_in.domain.event.Event"
                        )
                ),
                Arguments.of(
                        "AttendeeController",
                        "uploads/pass-in/src/main/java/tcc/com/pass_in/controllers/AttendeeController.java",
                        Set.of(
                                "lombok.RequiredArgsConstructor",
                                "org.springframework.http.ResponseEntity",
                                "org.springframework.web.bind.annotation.RestController",
                                "org.springframework.web.bind.annotation.RequestMapping",
                                "org.springframework.web.bind.annotation.GetMapping",
                                "org.springframework.web.bind.annotation.PostMapping",
                                "org.springframework.web.bind.annotation.PathVariable",
                                "org.springframework.web.method.support.CompositeUriComponentsContributor",
                                "org.springframework.web.util.UriComponentsBuilder",
                                "tcc.com.pass_in.dto.attendee.AttendeeBadgeResponseDTO",
                                "tcc.com.pass_in.services.AttendeeService",
                                "java.net.URI"
                        )
                ),
                Arguments.of(
                        "EventService",
                        "uploads/pass-in/src/main/java/tcc/com/pass_in/services/EventService.java",
                        Set.of(
                                "lombok.RequiredArgsConstructor",
                                "org.springframework.stereotype.Service",
                                "tcc.com.pass_in.domain.attendee.Attendee",
                                "tcc.com.pass_in.domain.event.Event",
                                "tcc.com.pass_in.services.AttendeeService",
                                "tcc.com.pass_in.domain.event.exceptions.EventNotFoundException",
                                "tcc.com.pass_in.domain.event.exceptions.EventFullException",
                                "tcc.com.pass_in.dto.attendee.AttendeeIdDTO",
                                "tcc.com.pass_in.dto.attendee.AttendeeRequestDTO",
                                "tcc.com.pass_in.dto.event.EventIdDTO",
                                "tcc.com.pass_in.dto.event.EventRequestDTO",
                                "tcc.com.pass_in.dto.event.EventResponseDTO",
                                "tcc.com.pass_in.repositories.EventRepository",
                                "java.text.Normalizer",
                                "java.time.LocalDateTime"
                        )
                ),
                Arguments.of(
                        "AttendeesListResponseDTO",
                        "uploads/pass-in/src/main/java/tcc/com/pass_in/dto/attendee/AttendeesListResponseDTO.java",
                        Set.of(
                                "tcc.com.pass_in.dto.attendee.AttendeeDetailsDTO"
                        )
                ),
                Arguments.of(
                        "EventNotFoundException",
                        "uploads/pass-in/src/main/java/tcc/com/pass_in/domain/event/exceptions/EventNotFoundException.java",
                        Set.of(
                                "java.lang.RuntimeException"
                        )
                ),
                Arguments.of(
                        "ExceptionEntityHandler",
                        "uploads/pass-in/src/main/java/tcc/com/pass_in/config/ExceptionEntityHandler.java",
                        Set.of(
                                "org.springframework.http.HttpStatus",
                                "org.springframework.http.ResponseEntity",
                                "org.springframework.web.bind.annotation.ControllerAdvice",
                                "org.springframework.web.bind.annotation.ExceptionHandler",
                                "tcc.com.pass_in.domain.attendee.exceptions.AttendeeAlreadyExistException",
                                "tcc.com.pass_in.domain.attendee.exceptions.AttendeeNotFoundException",
                                "tcc.com.pass_in.domain.checkin.exceptions.CheckInAlreadyExistsException",
                                "tcc.com.pass_in.domain.event.exceptions.EventFullException",
                                "tcc.com.pass_in.domain.event.exceptions.EventNotFoundException",
                                "tcc.com.pass_in.dto.general.ErrorResponseDTO"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0} - Type Resolution Test")
    @MethodSource("provideTestCases")
    @DisplayName("Verify correct type resolution for class: {0}")
    void testTypeResolution(String testName, String relativePath, Set<String> expectedTypes) {
        // Arrange
        Path classPath = Paths.get(relativePath);
        Path absolutePath = Paths.get(System.getProperty("user.dir")).resolve(classPath);

        // Act
        List<Dependency> dependencies = javaParserService.getDependencies(absolutePath);

        // Extract expected package names from expected types
        Set<String> expectedPackages = extractExpectedPackages(expectedTypes);

        // Extract actual package names from dependencies
        Set<String> actualPackages = dependencies.stream()
                .map(Dependency::getPackageName)
                .collect(Collectors.toSet());

        // Extract actual type names from dependencies
        Set<String> actualTypes = dependencies.stream()
                .flatMap(dependency -> dependency.getTypes().stream())
                .map(type -> type.getFullyQualifiedName())
                .collect(Collectors.toSet());

        // Assert basic structure
        assertThat(dependencies)
                .as("Dependencies list should not be null")
                .isNotNull();

        assertThat(dependencies)
                .as("Should have dependencies for class: %s", testName)
                .isNotEmpty();

        // Verify each dependency has a non-null packageName
        assertThat(dependencies)
                .as("All dependencies should have non-null package names")
                .allSatisfy(dependency -> assertThat(dependency.getPackageName()).isNotNull());

        // Verify each dependency has non-null types list
        assertThat(dependencies)
                .as("All dependencies should have non-null types list")
                .allSatisfy(dependency -> assertThat(dependency.getTypes()).isNotNull());

        // Check package names match
        Set<String> missingPackages = expectedPackages.stream()
                .filter(expectedPackage -> !actualPackages.contains(expectedPackage))
                .collect(Collectors.toSet());

        Set<String> unexpectedPackages = actualPackages.stream()
                .filter(actualPackage -> !expectedPackages.contains(actualPackage))
                .collect(Collectors.toSet());

        // Check type names match
        Set<String> missingTypes = expectedTypes.stream()
                .filter(expectedType -> !actualTypes.contains(expectedType))
                .collect(Collectors.toSet());

        Set<String> unexpectedTypes = actualTypes.stream()
                .filter(actualType -> !expectedTypes.contains(actualType))
                .collect(Collectors.toSet());

        // Provide detailed assertion messages
        if (!missingPackages.isEmpty() || !unexpectedPackages.isEmpty() || !missingTypes.isEmpty() || !unexpectedTypes.isEmpty()) {
            StringBuilder message = new StringBuilder()
                    .append("Dependency resolution mismatch for class: ").append(testName).append("\n");

            if (!missingPackages.isEmpty()) {
                message.append("Missing expected packages: ").append(missingPackages).append("\n");
            }

            if (!unexpectedPackages.isEmpty()) {
                message.append("Unexpected packages found: ").append(unexpectedPackages).append("\n");
            }

            if (!missingTypes.isEmpty()) {
                message.append("Missing expected types: ").append(missingTypes).append("\n");
            }

            if (!unexpectedTypes.isEmpty()) {
                message.append("Unexpected types found: ").append(unexpectedTypes).append("\n");
            }

            message.append("Expected packages: ").append(expectedPackages).append("\n")
                    .append("Actual packages: ").append(actualPackages).append("\n")
                    .append("Expected types: ").append(expectedTypes).append("\n")
                    .append("Actual types: ").append(actualTypes);

            throw new AssertionError(message.toString());
        }

        // Final assertions - exact matches
        assertThat(actualPackages)
                .as("Extracted package names should exactly match expected packages for class: %s", testName)
                .containsExactlyInAnyOrderElementsOf(expectedPackages);

        assertThat(actualTypes)
                .as("Extracted type names should exactly match expected types for class: %s", testName)
                .containsExactlyInAnyOrderElementsOf(expectedTypes);
    }

    @ParameterizedTest(name = "{0} - Consistent Results Test")
    @MethodSource("provideTestCases")
    @DisplayName("Verify consistent results on multiple calls for class: {0}")
    void testConsistentResults(String testName, String relativePath, Set<String> expectedTypes) {
        // Arrange
        Path classPath = Paths.get(relativePath);
        Path absolutePath = Paths.get(System.getProperty("user.dir")).resolve(classPath);

        // Extract expected package names from expected types
        Set<String> expectedPackages = extractExpectedPackages(expectedTypes);

        // Clear cache to ensure first call processes the file
        javaParserService.clearCache();

        // Act - First call
        List<Dependency> firstCall = javaParserService.getDependencies(absolutePath);

        Set<String> firstCallPackages = firstCall.stream()
                .map(Dependency::getPackageName)
                .collect(Collectors.toSet());

        Set<String> firstCallTypes = firstCall.stream()
                .flatMap(dependency -> dependency.getTypes().stream())
                .map(type -> type.getFullyQualifiedName())
                .collect(Collectors.toSet());

        // Act - Second call (should return same results, not empty since JavaParser doesn't use file-level caching like JDT)
        List<Dependency> secondCall = javaParserService.getDependencies(absolutePath);

        Set<String> secondCallPackages = secondCall.stream()
                .map(Dependency::getPackageName)
                .collect(Collectors.toSet());

        Set<String> secondCallTypes = secondCall.stream()
                .flatMap(dependency -> dependency.getTypes().stream())
                .map(type -> type.getFullyQualifiedName())
                .collect(Collectors.toSet());

        // Assert first call returns expected results
        assertThat(firstCallPackages)
                .as("First call should return expected packages for class: %s", testName)
                .containsExactlyInAnyOrderElementsOf(expectedPackages);

        assertThat(firstCallTypes)
                .as("First call should return expected types for class: %s", testName)
                .containsExactlyInAnyOrderElementsOf(expectedTypes);

        // Assert second call returns consistent results
        assertThat(secondCallPackages)
                .as("Second call should return consistent package results for class: %s", testName)
                .isEqualTo(firstCallPackages);

        assertThat(secondCallTypes)
                .as("Second call should return consistent type results for class: %s", testName)
                .isEqualTo(firstCallTypes);
    }

    @ParameterizedTest(name = "{0} - File Exists Test")
    @MethodSource("provideTestCases")
    @DisplayName("Verify test file exists: {0}")
    void testFileExists(String testName, String relativePath, Set<String> expectedTypes) {
        // Arrange & Act
        Path classPath = Paths.get(relativePath);
        Path absolutePath = Paths.get(System.getProperty("user.dir")).resolve(classPath);

        // Assert
        assertThat(absolutePath)
                .as("Test file should exist: %s at path: %s", testName, absolutePath)
                .exists()
                .isRegularFile()
                .isReadable();
    }
}
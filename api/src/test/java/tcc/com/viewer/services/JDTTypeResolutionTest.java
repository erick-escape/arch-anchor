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
 */
@SpringBootTest
@DisplayName("JDT Type Resolution Tests")
class JDTTypeResolutionTest {

    @Autowired
    private JDTParserService jdtParserService;

    @BeforeEach
    void setUp() {
        // Clear the processed files cache before each test to ensure consistent results
        jdtParserService.clearProcessedFilesCache();
    }

    /**
     * Provides test cases for parameterized tests.
     * Each argument contains: [test fullyQualifiedName, file path, expected fully qualified type names]
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
                                "tcc.com.pass_in.services.AttendeeService"
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
        List<Dependency> dependencies = jdtParserService.getDependencies(absolutePath);

        // Extract fully qualified type names from dependencies
        Set<String> actualTypes = dependencies.stream()
                .map(Dependency::getFullyQualifiedName)
                .collect(Collectors.toSet());

        // Assert
        assertThat(dependencies)
                .as("Dependencies list should not be null")
                .isNotNull();

        assertThat(dependencies)
                .as("Should have dependencies for class: %s", testName)
                .isNotEmpty();

        // Verify each dependency has a non-null fullyQualifiedName
        assertThat(dependencies)
                .as("All dependencies should have non-null names")
                .allSatisfy(dependency -> assertThat(dependency.getFullyQualifiedName()).isNotNull());

        // Check for missing expected types
        Set<String> missingTypes = expectedTypes.stream()
                .filter(expectedType -> !actualTypes.contains(expectedType))
                .collect(Collectors.toSet());

        // Check for unexpected types (dependencies found but not expected)
        Set<String> unexpectedTypes = actualTypes.stream()
                .filter(actualType -> !expectedTypes.contains(actualType))
                .collect(Collectors.toSet());

        // Provide detailed assertion messages
        if (!missingTypes.isEmpty() || !unexpectedTypes.isEmpty()) {
            StringBuilder message = new StringBuilder()
                    .append("Type resolution mismatch for class: ").append(testName).append("\n");

            if (!missingTypes.isEmpty()) {
                message.append("Missing expected types: ").append(missingTypes).append("\n");
            }

            if (!unexpectedTypes.isEmpty()) {
                message.append("Unexpected types found: ").append(unexpectedTypes).append("\n");
            }

            message.append("Expected types: ").append(expectedTypes).append("\n")
                    .append("Actual types: ").append(actualTypes);

            throw new AssertionError(message.toString());
        }

        // Final assertion - exact match
        assertThat(actualTypes)
                .as("Extracted types should exactly match expected types for class: %s", testName)
                .containsExactlyInAnyOrderElementsOf(expectedTypes);
    }

    @ParameterizedTest(name = "{0} - Consistent Results Test")
    @MethodSource("provideTestCases")
    @DisplayName("Verify consistent results on multiple calls for class: {0}")
    void testConsistentResults(String testName, String relativePath, Set<String> expectedTypes) {
        // Arrange
        Path classPath = Paths.get(relativePath);
        Path absolutePath = Paths.get(System.getProperty("user.dir")).resolve(classPath);

        // Clear cache to ensure first call processes the file
        jdtParserService.clearProcessedFilesCache();

        // Act - First call
        List<Dependency> firstCall = jdtParserService.getDependencies(absolutePath);
        Set<String> firstCallTypes = firstCall.stream()
                .map(Dependency::getFullyQualifiedName)
                .collect(Collectors.toSet());

        // Act - Second call (should return empty list due to caching)
        List<Dependency> secondCall = jdtParserService.getDependencies(absolutePath);

        // Assert
        assertThat(firstCallTypes)
                .as("First call should return expected types for class: %s", testName)
                .containsExactlyInAnyOrderElementsOf(expectedTypes);

        assertThat(secondCall)
                .as("Second call should return empty list due to processed files cache for class: %s", testName)
                .isEmpty();
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
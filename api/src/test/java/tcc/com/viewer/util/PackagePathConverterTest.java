package tcc.com.viewer.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringJUnitConfig
public class PackagePathConverterTest {
    private final PackageNameExtractor packageNameExtractor = new PackageNameExtractor();

    @Test
    void testExtractPackageName() {
        // Test main example
        String input = "tcc.com.viewer.domains.dependency.Dependency";
        String expected = "tcc.com.viewer.domains.dependency";
        String actual = packageNameExtractor.extractPackageName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testExtractPackageNameWithSingleClass() {
        String input = "MyClass";
        String expected = "";
        String actual = packageNameExtractor.extractPackageName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testExtractPackageNameWithEmptyString() {
        String input = "";
        String expected = "";
        String actual = packageNameExtractor.extractPackageName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testExtractPackageNameWithNull() {
        String input = null;
        String expected = "";
        String actual = packageNameExtractor.extractPackageName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testExtractPackageNameWithNestedPackage() {
        String input = "com.example.MyClass";
        String expected = "com.example";
        String actual = packageNameExtractor.extractPackageName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testAlternativeMethod() {
        String input = "tcc.com.viewer.domains.dependency.Dependency";
        String expected = "tcc.com.viewer.domains.dependency";
        String actual = packageNameExtractor.extractPackageNameAlternative(input);
        assertEquals(expected, actual);
    }
}
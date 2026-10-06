package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit test suite for EnumGenerator static methods.
 * Tests the core functionality of enum code generation:
 * - Enum constant normalization (toEnumConstant) - converts raw values to valid Java identifiers
 * - Enum source code generation (buildEnumSource) - produces compilable Java enum classes with all required components
 * - Edge cases including special characters, empty data, and error scenarios
 */
class EnumGeneratorTest {
    
    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1: toEnumConstant() - Enum Constant Normalization Tests
    // ─────────────────────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "firm-delivery,FIRM_DELIVERY",
        "Firm Delivery,FIRM_DELIVERY",
        "FIRM_DELIVERY,FIRM_DELIVERY",
        "firm delivery,FIRM_DELIVERY",
        "FirmDelivery,FIRMDELIVERY",
        "firm--delivery,FIRM_DELIVERY",
        "abc_def,ABC_DEF",
        "TYPE A,TYPE_A",
        "type-b---c,TYPE_B_C",
    })
    void testConvertToEnumConstantFromVariousFormats(final String input, final String expected) {
        final var result = EnumGenerator.toEnumConstant(input);

        assertThat("Value should be normalized to valid Java enum constant format",
                  result, is(expected));
    }

    @Test
    void testToEnumConstantLeadingDigit() {
        String input = "123ABC";

        final var result = EnumGenerator.toEnumConstant(input);

        assertThat("Leading digit should be prefixed with underscore for Java validity", 
                  result, is("_123ABC"));
    }

    @Test
    void testToEnumConstantSpecialCharactersOnly() {

        String input = "@#$%^&*()";

        final var exception = assertThrows(
            IllegalStateException.class,
            () -> EnumGenerator.toEnumConstant(input),
            "Special characters only should throw exception"
        );

        assertThat("Error message should indicate no alphanumeric characters",
            exception.getMessage(),
            containsString("contains no alphanumeric characters")
        );
    }

    @Test
    void testToEnumConstantNonAlphanumericOnly() {
        String input = "---___";

        final var exception = assertThrows(
            IllegalStateException.class,
            () -> EnumGenerator.toEnumConstant(input),
            "Non-alphanumeric only should throw exception"
        );

        assertThat("Error message should indicate cannot convert to valid Java constant",
            exception.getMessage(),
            containsString("cannot be converted to a valid Java enum constant")
        );
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2: buildEnumSource() - Enum Source Code Generation Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testBuildEnumSourceGeneratesValidCode() {
        final var enumName = "FormType";
        final var pkg = "uk.gov.companieshouse.enums";
        final var codes = new LinkedHashSet<>(Set.of("AD01", "CS01", "PSC01"));
        final var sourceFiles = Set.of("form-type-by-form-group.yaml");

        final var result = EnumGenerator.buildEnumSource(pkg, enumName, codes, sourceFiles);

        // Assert - verify generated enum contains all required components
        assertThat("Generated code should include package declaration",
                  result, containsString("package " + pkg + ";"));
        assertThat("Generated code should include enum keyword",
                  result, containsString("public enum " + enumName + " {"));
        assertThat("Generated code should include all enum constants",
                  result, containsString("AD01(\"AD01\")"));
        assertThat("Generated code should include all enum constants",
                  result, containsString("CS01(\"CS01\")"));
        assertThat("Generated code should include all enum constants",
                  result, containsString("PSC01(\"PSC01\")"));
        assertThat("Generated code should include JSON serialization annotation",
                  result, containsString("@JsonValue"));
        assertThat("Generated code should include JSON deserialization annotation",
                  result, containsString("@JsonCreator"));
        assertThat("Generated code should include code getter method",
                  result, containsString("public String code()"));
        assertThat("Generated code should include from() factory method",
                  result, containsString("public static FormType from(String v)"));
    }

    @Test
    void testBuildEnumSourceEmptyValuesThrows() {
        final var emptySet = new LinkedHashSet<String>();
        final var sourceFiles = Set.of("missing-data.yaml");

        final var exception = assertThrows(
            IllegalStateException.class,
            () -> EnumGenerator.buildEnumSource("uk.test", "EmptyEnum", emptySet, sourceFiles),
            "Empty enum values should throw exception"
        );

        // Verify error message provides useful diagnostic information
        assertThat("Error should mention no values extracted",
                  exception.getMessage(), containsString("no values extracted"));
        assertThat("Error should identify the source file",
                  exception.getMessage(), containsString("missing-data.yaml"));
    }

    @Test
    void testBuildEnumSourceDuplicationThrowsNormalizationCollisionError() {
        // Arrange: Two values normalize to same constant
        // "form-delivery" -> FORM_DELIVERY
        // "form delivery" -> FORM_DELIVERY (collision!)
        final var collidingSet = new LinkedHashSet<>(Set.of("form-delivery", "form delivery"));
        final var sourceFiles = Set.of("collision.yaml");

        final var exception = assertThrows(
            IllegalStateException.class,
            () -> EnumGenerator.buildEnumSource("uk.test", "CollisionEnum", collidingSet, sourceFiles),
            "Normalization collision should throw exception"
        );

        // Verify error message identifies the collision
        assertThat("Error should mention normalization collision",
                  exception.getMessage(), containsString("normalization collision"));
        assertThat("Error should identify the conflicting constant",
                  exception.getMessage(), containsString("FORM_DELIVERY"));
    }

    @Test
    void testBuildEnumSourceSpecialCharactersEscaped() {
        // Arrange: Value with quote that needs escaping
        final var codesWithQuote = new LinkedHashSet<>(Set.of("AD01"));
        final var sourceFiles = Set.of("test.yaml");

        final var result = EnumGenerator.buildEnumSource("uk.test", "TestEnum", codesWithQuote, sourceFiles);

        assertThat("Generated code should properly escape special characters",
                  result, containsString("AD01(\"AD01\")"));
    }

    @Test
    void testBuildEnumSourceFromMethodThrowsOnUnknownCode() {
        final var codes = new LinkedHashSet<>(Set.of("AD01"));
        final var sourceFiles = Set.of("test.yaml");

        final var result = EnumGenerator.buildEnumSource("uk.test", "TestEnum", codes, sourceFiles);

        // Assert - verify generated from() method includes error handling
        assertThat("Generated from() method should throw IllegalArgumentException for unknown values",
                  result, containsString("IllegalArgumentException"));
        assertThat("Error message should indicate unknown value",
                  result, containsString("Unknown"));
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3: Edge Cases and Error Scenarios
    // ─────────────────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"FormDelivery", "formdelivery"})
    void testToEnumConstantCaseInsensitive(final String input) {
        final var result = EnumGenerator.toEnumConstant(input);

        assertThat("All case variations should normalize to same uppercase constant",
                  result, is("FORMDELIVERY"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION: generateEnumsFromSources() - Multi-source enum file generation
    // ─────────────────────────────────────────────────────────────────────────

    @TempDir
    Path tempDir;

    private static EnumGeneratorConfig.Source valuesSource(final String rootKey) {
        final var enumDef = new EnumGeneratorConfig.EnumDefinition(
            EnumGeneratorConfig.Location.DICTIONARY_VALUES, null, null);
        return new EnumGeneratorConfig.Source(List.of(rootKey), null, null, Map.of("Statement", enumDef));
    }

    private EnumGenerator multiSourceGenerator() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root-a:\n  k1:\n    - alpha\n    - beta\n");
        Files.writeString(tempDir.resolve("b.yaml"), "root-b:\n  k2:\n    - beta\n    - gamma\n");
        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("a.yaml", valuesSource("root-a"));
        sources.put("b.yaml", valuesSource("root-b"));
        final var config = new EnumGeneratorConfig(
            new EnumGeneratorConfig.OutputPackage("uk.test.enums", "uk.test.bimaps"), sources);
        return new EnumGenerator(config, tempDir, tempDir.resolve("out"));
    }

    @Test
    void testMultiSourceEnumFileIncludesValuesFromAllSources() throws IOException {
        final var generator = multiSourceGenerator();

        generator.generateEnumsFromSources();

        final var source = Files.readString(tempDir.resolve("out/uk/test/enums/Statement.java"));
        assertThat("Values from the first source should be present", source, containsString("ALPHA(\"alpha\")"));
        assertThat("Shared values should be present", source, containsString("BETA(\"beta\")"));
        assertThat("Values from the later source should be written to the file", source, containsString("GAMMA(\"gamma\")"));
    }

    @Test
    void testMultiSourceEnumFileMatchesInMemoryValues() throws IOException {
        final var generator = multiSourceGenerator();

        generator.generateEnumsFromSources();

        final var source = Files.readString(tempDir.resolve("out/uk/test/enums/Statement.java"));
        assertThat("In-memory values should be the merged set",
            generator.getGeneratedEnums().get("Statement").values(), contains("alpha", "beta", "gamma"));
        generator.getGeneratedEnums().get("Statement").values().forEach(value ->
            assertThat("Every merged value should appear in the generated file",
                source, containsString("(\"" + value + "\")")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION: generateEnumsFromSources() - location, validation and error handling
    // ─────────────────────────────────────────────────────────────────────────

    private static EnumGeneratorConfig.Source sourceWith(
            final String rootKey, final EnumGeneratorConfig.Location location,
            final EnumGeneratorConfig.EnumValidation validation) {
        final var enumDef = new EnumGeneratorConfig.EnumDefinition(location, null, validation);
        return new EnumGeneratorConfig.Source(List.of(rootKey), null, null, Map.of("Statement", enumDef));
    }

    private EnumGenerator generatorFor(final Map<String, EnumGeneratorConfig.Source> sources) {
        final var config = new EnumGeneratorConfig(
            new EnumGeneratorConfig.OutputPackage("uk.test.enums", "uk.test.bimaps"), sources);
        return new EnumGenerator(config, tempDir, tempDir.resolve("out"));
    }

    private static EnumGeneratorConfig.EnumValidation uniqueness(final EnumGeneratorConfig.UniquenessValidation mode) {
        return new EnumGeneratorConfig.EnumValidation(mode, null);
    }

    @Test
    void testDictionaryKeysLocationExtractsKeysOnly() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root:\n  k1:\n    - v1\n  k2:\n    - v2\n");
        final var generator = generatorFor(Map.of("a.yaml", sourceWith("root", EnumGeneratorConfig.Location.DICTIONARY_KEYS, null)));

        generator.generateEnumsFromSources();

        assertThat(generator.getGeneratedEnums().get("Statement").values(), contains("k1", "k2"));
        final var source = Files.readString(tempDir.resolve("out/uk/test/enums/Statement.java"));
        assertThat(source, containsString("K1(\"k1\")"));
        assertThat(source, not(containsString("V1")));
    }

    @Test
    void testEnumWithoutValidationIsGenerated() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root:\n  k1:\n    - v1\n    - v1\n");
        final var generator = generatorFor(Map.of("a.yaml", sourceWith("root", EnumGeneratorConfig.Location.DICTIONARY_VALUES, null)));

        generator.generateEnumsFromSources();

        assertThat("Duplicates are collapsed when no uniqueness validation is configured",
            generator.getGeneratedEnums().get("Statement").values(), contains("v1"));
    }

    @Test
    void testValidationWithoutUniquenessIsIgnored() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root:\n  k1:\n    - v1\n    - v1\n");
        final var generator = generatorFor(Map.of("a.yaml",
            sourceWith("root", EnumGeneratorConfig.Location.DICTIONARY_VALUES, new EnumGeneratorConfig.EnumValidation(null, true))));

        generator.generateEnumsFromSources();

        assertThat(generator.getGeneratedEnums().get("Statement").values(), contains("v1"));
    }

    @Test
    void testUniquenessViolationInFirstSourceIsReported() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root:\n  k1:\n    - v1\n    - v1\n");
        final var generator = generatorFor(Map.of("a.yaml", sourceWith("root",
            EnumGeneratorConfig.Location.DICTIONARY_VALUES, uniqueness(EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY))));

        final var exception = assertThrows(IllegalStateException.class, generator::generateEnumsFromSources);

        assertThat(exception.getMessage(), containsString("VALUES_PER_KEY"));
        assertThat(exception.getMessage(), containsString("a.yaml"));
    }

    @Test
    void testUniquenessViolationInMergedSourceIsReported() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root-a:\n  k1:\n    - v1\n");
        Files.writeString(tempDir.resolve("b.yaml"), "root-b:\n  k1:\n    - v2\n    - v2\n");
        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("a.yaml", sourceWith("root-a", EnumGeneratorConfig.Location.DICTIONARY_VALUES, null));
        sources.put("b.yaml", sourceWith("root-b", EnumGeneratorConfig.Location.DICTIONARY_VALUES,
            uniqueness(EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY)));
        final var generator = generatorFor(sources);

        final var exception = assertThrows(IllegalStateException.class, generator::generateEnumsFromSources);

        assertThat(exception.getMessage(), containsString("b.yaml"));
    }

    @Test
    void testMergedSourceWithoutValidationIsAccepted() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root-a:\n  k1:\n    - v1\n");
        Files.writeString(tempDir.resolve("b.yaml"), "root-b:\n  k1:\n    - v2\n    - v2\n");
        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("a.yaml", sourceWith("root-a", EnumGeneratorConfig.Location.DICTIONARY_VALUES, null));
        sources.put("b.yaml", sourceWith("root-b", EnumGeneratorConfig.Location.DICTIONARY_VALUES,
            new EnumGeneratorConfig.EnumValidation(null, null)));
        final var generator = generatorFor(sources);

        generator.generateEnumsFromSources();

        assertThat(generator.getGeneratedEnums().get("Statement").values(), contains("v1", "v2"));
    }

    @Test
    void testMissingYamlFileThrowsIoException() {
        final var generator = generatorFor(Map.of("missing.yaml", sourceWith("root", EnumGeneratorConfig.Location.DICTIONARY_VALUES, null)));

        assertThrows(NoSuchFileException.class, generator::generateEnumsFromSources);
    }

    @Test
    void testUnwritableOutputDirectoryThrowsIoException() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root:\n  k1:\n    - v1\n");
        Files.writeString(tempDir.resolve("out"), "a file where the output directory should be");
        final var generator = generatorFor(Map.of("a.yaml", sourceWith("root", EnumGeneratorConfig.Location.DICTIONARY_VALUES, null)));

        assertThrows(IOException.class, generator::generateEnumsFromSources);
    }

    @Test
    void testMergedEnumFileWriteFailureThrowsIoException() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root-a:\n  k1:\n    - v1\n");
        Files.writeString(tempDir.resolve("b.yaml"), "root-b:\n  k1:\n    - v2\n");
        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("a.yaml", sourceWith("root-a", EnumGeneratorConfig.Location.DICTIONARY_VALUES, null));
        sources.put("b.yaml", sourceWith("root-b", EnumGeneratorConfig.Location.DICTIONARY_VALUES, null));
        final var generator = generatorFor(sources);
        final var enumFile = tempDir.resolve("out/uk/test/enums/Statement.java");

        // Replace the enum file written for the first source with a directory so the rewrite fails
        final var failing = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        failing.put("a.yaml", sources.get("a.yaml"));
        generatorFor(failing).generateEnumsFromSources();
        Files.delete(enumFile);
        Files.createDirectory(enumFile);

        assertThrows(IOException.class, generator::generateEnumsFromSources);
    }

    @Test
    void testGetValidatorSharesGeneratedEnumsWithGenerator() throws IOException {
        Files.writeString(tempDir.resolve("a.yaml"), "root:\n  k1:\n");
        final var enumDef = new EnumGeneratorConfig.EnumDefinition(EnumGeneratorConfig.Location.DICTIONARY_KEYS, null, null);
        final var source = new EnumGeneratorConfig.Source(List.of("root"), "Statement", null, Map.of("Statement", enumDef));
        final var generator = generatorFor(Map.of("a.yaml", source));

        assertThat(generator.getValidator(), is(notNullValue()));
        generator.generateEnumsFromSources();

        // k1 is only recognised as a Statement key if the validator sees the generator's enums
        assertDoesNotThrow(generator.getValidator()::validateNoForeignEnumValuesInSources);
    }
}

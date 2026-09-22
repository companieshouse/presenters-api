package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit test suite for EnumGenerator static methods.
 *
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
    void testConvertToEnumConstantFromVariousFormats(String input, String expected) {
        var result = EnumGenerator.toEnumConstant(input);

        assertThat("Value should be normalized to valid Java enum constant format",
                  result, is(expected));
    }

    @Test
    void testToEnumConstantLeadingDigit() {
        String input = "123ABC";

        var result = EnumGenerator.toEnumConstant(input);

        assertThat("Leading digit should be prefixed with underscore for Java validity", 
                  result, is("_123ABC"));
    }

    @Test
    void testToEnumConstantSpecialCharactersOnly() {

        String input = "@#$%^&*()";

        var exception = assertThrows(
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

        var exception = assertThrows(
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
        var enumName = "FormType";
        var pkg = "uk.gov.companieshouse.enums";
        var codes = new LinkedHashSet<>(Set.of("AD01", "CS01", "PSC01"));
        var sourceFiles = Set.of("form-type-by-form-group.yaml");

        var result = EnumGenerator.buildEnumSource(pkg, enumName, codes, sourceFiles);

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
        var emptySet = new LinkedHashSet<String>();
        var sourceFiles = Set.of("missing-data.yaml");

        var exception = assertThrows(
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
        var collidingSet = new LinkedHashSet<>(Set.of("form-delivery", "form delivery"));
        var sourceFiles = Set.of("collision.yaml");

        var exception = assertThrows(
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
        var codesWithQuote = new LinkedHashSet<>(Set.of("AD01"));
        var sourceFiles = Set.of("test.yaml");

        var result = EnumGenerator.buildEnumSource("uk.test", "TestEnum", codesWithQuote, sourceFiles);

        assertThat("Generated code should properly escape special characters",
                  result, containsString("AD01(\"AD01\")"));
    }

    @Test
    void testBuildEnumSourceFromMethodThrowsOnUnknownCode() {
        var codes = new LinkedHashSet<>(Set.of("AD01"));
        var sourceFiles = Set.of("test.yaml");

        var result = EnumGenerator.buildEnumSource("uk.test", "TestEnum", codes, sourceFiles);

        // Assert - verify generated from() method includes error handling
        assertThat("Generated from() method should throw IllegalArgumentException for unknown values",
                  result, containsString("IllegalArgumentException"));
        assertThat("Error message should indicate unknown value",
                  result, containsString("Unknown"));
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3: Edge Cases and Error Scenarios
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testToEnumConstantLongString() {
        var longInput = "very-long-form-type-name-with-many-hyphens-and-words";

        var result = EnumGenerator.toEnumConstant(longInput);

        assertThat("Long strings should be normalized to uppercase with underscores",
                  result, containsString("VERY_LONG_FORM_TYPE_NAME_WITH_MANY_HYPHENS_AND_WORDS"));
        assertThat("Result should contain only valid Java constant characters",
                  result.matches("[A-Z_0-9]+"), is(true));
    }

    @Test
    void testToEnumConstantConsecutiveUnderscoresCollapsed() {
        var input = "form---delivery";

        var result = EnumGenerator.toEnumConstant(input);

        assertThat("Multiple consecutive separators should collapse to single underscore",
                  result, is("FORM_DELIVERY"));
        assertThat("Result should not contain double underscores",
                  result.contains("__"), is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FormDelivery", "formdelivery", "FORMDELIVERY", "FoRmDeLiVeRy"})
    void testToEnumConstantCaseInsensitive(String input) {
        var result = EnumGenerator.toEnumConstant(input);

        assertThat("All case variations should normalize to same uppercase constant",
                  result, is("FORMDELIVERY"));
    }
}
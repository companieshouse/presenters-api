package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit test suite for GeneratorValidator.validateUniqueness().
 *
 * Tests the core uniqueness validation for dictionary values:
 * - VALUES_PER_KEY_SET mode - each value appears under AT MOST ONE key
 * - VALUES_PER_KEY mode - each value appears AT MOST ONCE under each key
 * - Valid data that passes both constraints
 * - Invalid data detecting violations with clear error messages
 */
class GeneratorValidatorTest {

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1: VALUES_PER_KEY_SET Mode - Singular Reverse Mapping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testValidateUniquenessValidDataValuePerKeySet() {
        var dataMap = Map.of(
            "firm-delivery", List.of("AD01", "CS01"),
            "individual-delivery", List.of("PSC01")
        );

        assertDoesNotThrow(
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET,
                "test.yaml"
            ),
            "Should allow each value to appear under exactly one key"
        );
    }

    @Test
    void testValidateUniquenessViolationValueUnderMultipleKeysValuePerKeySet() {
        var dataMap = Map.of(
            "firm-delivery", List.of("AD01", "CS01"),
            "individual-delivery", List.of("AD01", "PSC01")
        );

        var exception = assertThrows(
            IllegalStateException.class,
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET,
                "test.yaml"
            ),
            "Should throw when value appears under multiple keys"
        );

        assertThat("Error should mention constraint VALUES_PER_KEY_SET",
                  exception.getMessage(), containsString("VALUES_PER_KEY_SET"));
        assertThat("Error should identify conflicting value",
                  exception.getMessage(), containsString("AD01"));
        assertThat("Error should mention appears under multiple dictionary keys",
                  exception.getMessage(), containsString("appears under multiple dictionary keys"));
    }

    @Test
    void testValidateUniquenessEmptyValuesValuePerKeySet() {
        var dataMap = Map.of(
            "firm-delivery", List.of(""),
            "individual-delivery", List.of("AD01")
        );

        assertDoesNotThrow(
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET,
                "test.yaml"
            ),
            "Should handle keys with empty value lists"
        );
    }

    @Test
    void testValidateUniquenessMultipleViolationsValuePerKeySet() {
        var dataMap = Map.of(
            "key1", List.of("AD01", "CS01"),
            "key2", List.of("AD01", "PSC01"),
            "key3", List.of("CS01")
        );

        var exception = assertThrows(
            IllegalStateException.class,
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET,
                "test.yaml"
            ),
            "Should throw on first violation found"
        );

        var message = exception.getMessage();
        var hasConflict = message.contains("AD01") || message.contains("CS01");
        assertThat("Error should identify a conflicting value",
                  hasConflict, is(true));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2: VALUES_PER_KEY Mode
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testValidateUniquenessValidDataValuePerKey() {
        var dataMap = Map.of(
            "firm-delivery", List.of("officer-employee", "corporate-officer"),
            "individual-delivery", List.of("officer-employee", "acsp-sole-trader")
        );

        assertDoesNotThrow(
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY,
                "test.yaml"
            ),
            "Should allow values to repeat across different keys"
        );
    }

    @Test
    void testValidateUniquenessViolationDuplicateWithinKeyValuePerKey() {
        var dataMap = Map.of(
            "firm-delivery", List.of("officer-employee", "officer-employee", "corporate-officer"),
            "individual-delivery", List.of("officer-employee")
        );

        var exception = assertThrows(
            IllegalStateException.class,
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY,
                "test.yaml"
            ),
            "Should throw when value appears multiple times under same key"
        );

        assertThat("Error should mention constraint VALUES_PER_KEY",
                  exception.getMessage(), containsString("VALUES_PER_KEY"));
        assertThat("Error should identify duplicated value",
                  exception.getMessage(), containsString("officer-employee"));
        assertThat("Error should identify the key",
                  exception.getMessage(), containsString("firm-delivery"));
        assertThat("Error should mention appears multiple times under key",
                  exception.getMessage(), containsString("appears multiple times under key"));
    }

    @Test
    void testValidateUniquenessValueRepeatingAcrossKeysValuePerKey() {
        var dataMap = Map.of(
            "key1", List.of("A", "B", "C"),
            "key2", List.of("A", "D"),
            "key3", List.of("A", "B")
        );

        assertDoesNotThrow(
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY,
                "test.yaml"
            ),
            "Should allow same value across multiple different keys"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3: Edge Cases and Boundary Conditions
    // ─────────────────────────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(EnumGeneratorConfig.UniquenessValidation.class)
    void testValidateEmptyMap(EnumGeneratorConfig.UniquenessValidation mode) {
        Map<String, List<String>> dataMap = Map.of();

        assertDoesNotThrow(
                () -> GeneratorValidator.validateUniqueness(
                        dataMap,
                        mode,
                        "empty.yaml"
                ),
                "Should handle empty map without errors for mode: " + mode
        );
    }

    @Test
    void testValidateUniquenessSingleKeyValuePerKeySet() {
        var dataMap = Map.of("only-key", List.of("A", "B", "C"));

        assertDoesNotThrow(
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET,
                "test.yaml"
            ),
            "Should pass when single key has multiple distinct values"
        );
    }

    @Test
    void testValidateUniquenessNullValidationParameter() {
        var dataMap = Map.of("key", List.of("value"));

        assertDoesNotThrow(
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                null,
                "test.yaml"
            ),
            "Should safely handle null uniqueness validation (no-op)"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 4: Real-world Scenarios
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testValidateUniquenessFormTypeScenarioValid() {
        var dataMap = Map.of(
            "firm-delivery", List.of("AD01", "AD02"),
            "individual-delivery", List.of("CS01", "CS02"),
            "lp-delivery", List.of("PSC01")
        );

        assertDoesNotThrow(
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET,
                "form-type-by-form-group.yaml"
            ),
            "Should pass valid form type data where each form belongs to one group"
        );
    }

    @Test
    void testValidateUniquenessPresenterTypeScenarioValid() {
        var dataMap = Map.of(
            "firm-delivery", List.of("officer-employee", "corporate-officer-employee", "acsp-sole-trader"),
            "individual-delivery", List.of("officer-employee", "acsp-sole-trader"),
            "lp-delivery", List.of("corporate-officer-employee")
        );

        assertDoesNotThrow(
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY,
                "presenter-type-by-form-group.yaml"
            ),
            "Should pass when same presenter type can serve multiple form groups"
        );
    }

    @ParameterizedTest
    @CsvSource({
        "firm-delivery,AD01,individual-delivery,AD01",
        "key1,valueA,key2,valueA",
        "alpha,item,beta,item",
        "key1,value,key2,value"
    })
    void testValidateUniquenessViolationPatternValuePerKeySet(
            String key1, String value1, String key2, String value2) {
        var dataMap = Map.of(
            key1, List.of(value1),
            key2, List.of(value2)
        );

        var exception = assertThrows(
            IllegalStateException.class,
            () -> GeneratorValidator.validateUniqueness(
                dataMap,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET,
                "test.yaml"
            ),
            "Should detect value appearing under multiple keys"
        );

        assertThat("Error should mention the violating value",
                  exception.getMessage(), containsString(value1));
    }
}
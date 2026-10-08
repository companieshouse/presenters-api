package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.dataformat.yaml.JacksonYAMLParseException;

/**
 * Unit test suite for EnumBiMapGenerator utility methods.
 * Tests pure utility functions for YAML data handling and escaping:
 * - String escaping for Java literals
 * - List-to-set data conversion
 * - Value extraction and validation from YAML
 */
class EnumBiMapGeneratorHelpersTest {

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1: escapeForJavaStringLiteral() - String Escaping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testEscapeForJavaStringLiteralPlainText() {
        final var input = "plain text";

        final var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Plain text should pass through unchanged",
                  result, is("plain text"));
    }

    @Test
    void testEscapeForJavaStringLiteralWithDoubleQuotes() {
        final var input = "text with \"quotes\"";

        final var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Double quotes should be escaped",
                  result, containsString("\\\""));
    }

    @Test
    void testEscapeForJavaStringLiteralWithBackslash() {
        final var input = "path\\to\\file";

        final var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Backslashes should be escaped",
                  result, containsString("\\\\"));
    }

    @Test
    void testEscapeForJavaStringLiteralWithBothQuotesAndBackslash() {
        final var input = "text with \"quotes\" and \\slashes";

        final var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Should escape both quotes and backslashes",
                  result, containsString("\\\""));
        assertThat("Should escape both quotes and backslashes",
                  result, containsString("\\\\"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc"})
    void testEscapeForJavaStringLiteralNoSpecialChars(final String input) {
        final var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Strings without special chars should pass through",
                  result, is(input));
    }

    @Test
    void testEscapeForJavaStringLiteralMixedContent() {
        final var input = "JSON: {\"key\": \"value\"}";

        final var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Should escape quotes in JSON-like content",
                  result, containsString("\\\""));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2: toSetMap() - List to Set Conversion
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testToSetMapSimpleConversion() {
        final var listMap = Map.of(
            "key1", List.of("a", "b", "c"),
            "key2", List.of("x", "y")
        );

        final var result = EnumBiMapGenerator.toSetMap(listMap);

        assertThat("Result should have same keys",
                  result.keySet(), hasItems("key1", "key2"));
        assertThat("Values should be sets",
                  result.get("key1"), instanceOf(Set.class));
    }

    @Test
    void testToSetMapDeduplicatesValues() {
        final var listMap = Map.of(
            "key1", List.of("a", "b", "a", "c", "b")
        );

        final var result = EnumBiMapGenerator.toSetMap(listMap);

        assertThat("Set should deduplicate values",
                  result.get("key1").size(), is(3));
        assertThat("Set should contain unique values",
                  result.get("key1"), hasItems("a", "b", "c"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3: extractValuesList() - Value Extraction and Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testExtractValuesListSimpleList() {
        final var entry = new AbstractMap.SimpleEntry<String, Object>("key", List.of("a", "b", "c"));

        final var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("Should extract list values",
                  result, hasItems("a", "b", "c"));
        assertThat("Result should have same size",
                  result.size(), is(3));
    }

    @Test
    void testExtractValuesListNullValue() {
        final var entry = new AbstractMap.SimpleEntry<>("key", null);

        final var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("Null value should return empty list",
                  result.size(), is(0));
    }

    @Test
    void testExtractValuesListEmptyList() {
        final var entry = new AbstractMap.SimpleEntry<String, Object>("key", List.of());

        final var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("Empty list should return empty list",
                  result.size(), is(0));
    }

    @Test
    void testExtractValuesListStringValueThrows() {
        final var entry = new AbstractMap.SimpleEntry<String, Object>("key", "string-value");

        final var exception = assertThrows(
                IllegalStateException.class,
                () -> EnumBiMapGenerator.extractValuesList(entry, "key1", "test.yaml"),
                "Should throw on invalid structure"
        );
        assertThat(exception.getMessage(), containsString("expected a list of strings or null/empty value"));
    }

    @Test
    void testExtractValuesListInvalidTypeThrows() {
        final var entry = new AbstractMap.SimpleEntry<String, Object>("key1", 12345);

        final var exception = assertThrows(
            IllegalStateException.class,
            () -> EnumBiMapGenerator.extractValuesList(entry, "key1", "test.yaml"),
            "Should throw on invalid structure"
        );

        assertThat("Error should mention data structure error",
                  exception.getMessage(), containsString("Data structure error"));
        assertThat("Error should identify the key",
                  exception.getMessage(), containsString("key1"));
    }

    @Test
    void testExtractValuesListInvalidListItemTypeThrows() {
        final var entry = new AbstractMap.SimpleEntry<String, Object>("key", List.of("valid", 123, "more"));

        final var exception = assertThrows(
            IllegalStateException.class,
            () -> EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml"),
            "Should throw when list contains non-string"
        );

        assertThat("Error should mention data type error",
                  exception.getMessage(), containsString("Data type error"));
        assertThat("Error should identify invalid type",
                  exception.getMessage(), containsString("Integer"));
    }

    @Test
    void testExtractValuesListPreservesOrderAndDuplicates() {
        final var entry = new AbstractMap.SimpleEntry<String, Object>("key", List.of("a", "b", "a", "c"));

        final var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("Should preserve order",
                  result, contains("a", "b", "a", "c"));
        assertThat("Should preserve duplicates",
                  result.size(), is(4));
    }

    @Test
    void testExtractValuesListFromMapWithNestedList() {
        final var entry = new AbstractMap.SimpleEntry<String, Object>("forms", List.of("AD01", "CS01", "PSC01"));

        final var result = EnumBiMapGenerator.extractValuesList(entry, "forms", "form-data.yaml");

        assertThat("Should extract form codes",
                  result, hasItems("AD01", "CS01", "PSC01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 4: readConfig() - Configuration File Loading
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testReadConfigValidFile() throws IOException {
        final var configFile = Paths.get("src/test/resources/valid-config.yaml");
        final var result = EnumBiMapGenerator.readConfig(configFile);

        assertThat("Config should load successfully", result, is(notNullValue()));
        assertThat("Configuration should have output packages", result.outputPackage(), is(notNullValue()));
    }

    @Test
    void testReadConfigMissingFileException() {
        final var configFile = Paths.get("src/test/resources/nonexistent-config.yaml");
        final var exception = assertThrows(
                IOException.class,
                () -> EnumBiMapGenerator.readConfig(configFile),
                "Should throw when config file doesn't exist"
        );
        assertThat("Error message should be informative",
                exception.getMessage(), equalTo("src/test/resources/nonexistent-config.yaml"));
    }

    @Test
    void testReadConfigMalformedYamlException() {
        final var configFile = Paths.get("src/test/resources/malformed.yaml");
        final var exception = assertThrows(
                JacksonYAMLParseException.class,
                () -> EnumBiMapGenerator.readConfig(configFile),
                "Should throw when config file is malformed"
        );
        assertThat("Error message should be informative",
                exception.getMessage(), containsString("could not find expected ':'"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 5: readYamlData() - YAML Data Reading and Navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testReadYamlDataSimpleRootKey() throws IOException {
        final var yamlFile = Paths.get("src/test/resources/valid-data.yaml");
        final var rootKeys = List.of("data");

        final var result = EnumBiMapGenerator.readYamlData(yamlFile, rootKeys);

        assertThat("Should return map with keys",
                result.keySet(), hasItems("key1", "key2"));
        assertThat("Should preserve list values",
                result.get("key1"), contains("value1", "value2"));
        assertThat("Should preserve list values",
                result.get("key2"), contains("value3"));
    }

    @Test
    void testReadYamlDataMultiLevelRootKeys() throws IOException {
        final var yamlFile = Paths.get("src/test/resources/nested-data.yaml");
        final var rootKeys = List.of("level1", "level2");

        final var result = EnumBiMapGenerator.readYamlData(yamlFile, rootKeys);

        assertThat("Should navigate nested structure",
                result.keySet(), hasItems("nestedKey1", "nestedKey2"));
        assertThat("Should extract values from nested level",
                result.get("nestedKey1"), contains("nestedValue1"));
        assertThat("Should extract values from nested level",
                result.get("nestedKey2"), contains("nestedValue2"));
    }

    @Test
    void testReadYamlDataMissingFirstRootKeyThrows(){
        final var yamlFile = Paths.get("src/test/resources/valid-data.yaml");
        final var rootKeys = List.of("missing");

        final var exception = assertThrows(
                IllegalStateException.class,
                () -> EnumBiMapGenerator.readYamlData(yamlFile, rootKeys),
                "Should throw when first root-key missing"
        );
        assertThat("Error should mention missing root-key",
                exception.getMessage(), containsString("missing root-key"));
    }

    @Test
    void testReadYamlDataMissingNestedRootKeyThrows(){
        final var yamlFile = Paths.get("src/test/resources/nested-data.yaml");
        final var rootKeys = List.of("level1", "missing");

        final var exception = assertThrows(
                IllegalStateException.class,
                () -> EnumBiMapGenerator.readYamlData(yamlFile, rootKeys),
                "Should throw when nested root-key missing"
        );

        assertThat("Error should mention missing root-key",
                exception.getMessage(), containsString("missing root-keys"));
    }

    @Test
    void testReadYamlDataPreservesInsertionOrder() throws IOException {
        final var yamlFile = Paths.get("src/test/resources/valid-data.yaml");
        final var rootKeys = List.of("data");

        final var result = EnumBiMapGenerator.readYamlData(yamlFile, rootKeys);

        // LinkedHashMap should preserve order
        final var keys = new ArrayList<>(result.keySet());
        assertThat("Should maintain insertion order",
                keys, contains("key1", "key2"));
    }

    @Test
    void testReadYamlDataMissingFileThrows() {
        final var yamlFile = Paths.get("src/test/resources/non-existent.yaml");
        final var rootKeys = List.of("data");

        final var exception = assertThrows(
                IOException.class,
                () -> EnumBiMapGenerator.readYamlData(yamlFile, rootKeys),
                "Should throw when YAML file doesn't exist"
        );
        assertThat("Error should include path to missing file",
                exception.getMessage(), Matchers.equalTo("src/test/resources/non-existent.yaml"));
    }

    @Test
    void testReadYamlDataMalformedYamlThrows() {
        final var yamlFile = Paths.get("src/test/resources/malformed.yaml");
        final var rootKeys = List.of("data");

        final var exception = assertThrows(
                Exception.class,
                () -> EnumBiMapGenerator.readYamlData(yamlFile, rootKeys),
                "Should throw on malformed data YAML"
        );
        assertThat("Error should return an informative exception",
                exception.getMessage(), containsString("could not find expected"));
    }
}


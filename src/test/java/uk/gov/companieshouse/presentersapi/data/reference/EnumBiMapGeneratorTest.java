package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit test suite for EnumBiMapGenerator utility methods.
 * Tests pure utility functions for YAML data handling and escaping:
 * - String escaping for Java literals
 * - List-to-set data conversion
 * - Value extraction and validation from YAML
 */
class EnumBiMapGeneratorTest {

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1: escapeForJavaStringLiteral() - String Escaping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testEscapeForJavaStringLiteralPlainText() {
        var input = "plain text";

        var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Plain text should pass through unchanged",
                  result, is("plain text"));
    }

    @Test
    void testEscapeForJavaStringLiteralWithDoubleQuotes() {
        var input = "text with \"quotes\"";

        var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Double quotes should be escaped",
                  result, containsString("\\\""));
    }

    @Test
    void testEscapeForJavaStringLiteralWithBackslash() {
        var input = "path\\to\\file";

        var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Backslashes should be escaped",
                  result, containsString("\\\\"));
    }

    @Test
    void testEscapeForJavaStringLiteralWithBothQuotesAndBackslash() {
        var input = "text with \"quotes\" and \\slashes";

        var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Should escape both quotes and backslashes",
                  result, containsString("\\\""));
        assertThat("Should escape both quotes and backslashes",
                  result, containsString("\\\\"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "abc", "123"})
    void testEscapeForJavaStringLiteralNoSpecialChars(String input) {
        var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Strings without special chars should pass through",
                  result, is(input));
    }

    @Test
    void testEscapeForJavaStringLiteralMixedContent() {
        var input = "JSON: {\"key\": \"value\"}";

        var result = EnumBiMapGenerator.escapeForJavaStringLiteral(input);

        assertThat("Should escape quotes in JSON-like content",
                  result, containsString("\\\""));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2: toSetMap() - List to Set Conversion
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testToSetMapSimpleConversion() {
        var listMap = Map.of(
            "key1", List.of("a", "b", "c"),
            "key2", List.of("x", "y")
        );

        var result = EnumBiMapGenerator.toSetMap(listMap);

        assertThat("Result should have same keys",
                  result.keySet(), hasItems("key1", "key2"));
        assertThat("Values should be sets",
                  result.get("key1"), instanceOf(Set.class));
    }

    @Test
    void testToSetMapDeduplicatesValues() {
        var listMap = Map.of(
            "key1", List.of("a", "b", "a", "c", "b")
        );

        var result = EnumBiMapGenerator.toSetMap(listMap);

        assertThat("Set should deduplicate values",
                  result.get("key1").size(), is(3));
        assertThat("Set should contain unique values",
                  result.get("key1"), hasItems("a", "b", "c"));
    }

    @Test
    void testToSetMapEmptyValues() {
        var listMap = Map.of(
            "key1", List.<String>of()
        );

        var result = EnumBiMapGenerator.toSetMap(listMap);

        assertThat("Result should contain key",
                  result.containsKey("key1"), is(true));
        assertThat("Value should be empty set",
                  result.get("key1").size(), is(0));
    }

    @Test
    void testToSetMapPreservesOrder() {
        var listMap = Map.of(
            "z-key", List.of("value1"),
            "a-key", List.of("value2"),
            "m-key", List.of("value3")
        );

        var result = EnumBiMapGenerator.toSetMap(listMap);

        assertThat("Result keys should maintain insertion order",
                  result.keySet().stream().toList(),
                  hasItems("z-key", "a-key", "m-key"));
    }

    @Test
    void testToSetMapEmptyMap() {
        Map<String, List<String>> listMap = Map.of();

        var result = EnumBiMapGenerator.toSetMap(listMap);

        assertThat("Empty map should result in empty set map",
                  result.size(), is(0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3: extractValuesList() - Value Extraction and Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testExtractValuesListSimpleList() {
        var entry = new AbstractMap.SimpleEntry<String, Object>("key", List.of("a", "b", "c"));

        var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("Should extract list values",
                  result, hasItems("a", "b", "c"));
        assertThat("Result should have same size",
                  result.size(), is(3));
    }

    @Test
    void testExtractValuesListNullValue() {
        var entry = new AbstractMap.SimpleEntry<String, Object>("key", null);

        var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("Null value should return empty list",
                  result.size(), is(0));
    }

    @Test
    void testExtractValuesListEmptyList() {
        var entry = new AbstractMap.SimpleEntry<String, Object>("key", List.of());

        var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("Empty list should return empty list",
                  result.size(), is(0));
    }

    @Test
    void testExtractValuesListStringValueIgnored() {
        var entry = new AbstractMap.SimpleEntry<String, Object>("key", "string-value");

        var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("String value should return empty list",
                  result.size(), is(0));
    }

    @Test
    void testExtractValuesListInvalidTypeThrows() {
        var entry = new AbstractMap.SimpleEntry<String, Object>("key1", 12345);

        var exception = assertThrows(
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
        var entry = new AbstractMap.SimpleEntry<String, Object>("key", List.of("valid", 123, "more"));

        var exception = assertThrows(
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
        var entry = new AbstractMap.SimpleEntry<String, Object>("key", List.of("a", "b", "a", "c"));

        var result = EnumBiMapGenerator.extractValuesList(entry, "key", "test.yaml");

        assertThat("Should preserve order",
                  result, contains("a", "b", "a", "c"));
        assertThat("Should preserve duplicates",
                  result.size(), is(4));
    }

    @Test
    void testExtractValuesListFromMapWithNestedList() {
        var entry = new AbstractMap.SimpleEntry<String, Object>("forms", List.of("AD01", "CS01", "PSC01"));

        var result = EnumBiMapGenerator.extractValuesList(entry, "forms", "form-data.yaml");

        assertThat("Should extract form codes",
                  result, hasItems("AD01", "CS01", "PSC01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 4: readConfig() - Configuration File Loading
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testReadConfigValidFile() throws IOException, NoSuchFileException {
        var configFile = Paths.get("src/test/resources/valid-config.yaml");
        var result = EnumBiMapGenerator.readConfig(configFile);

        assertThat("Config should load successfully", result, notNullValue());
        assertThat("Configuration should have output packages", result.outputPackage(), notNullValue());
    }

    @Test
    void testReadConfigMissingFileException() {
        var configFile = Paths.get("src/test/resources/nonexistent-config.yaml");
        var exception = assertThrows(
                IOException.class,
                () -> EnumBiMapGenerator.readConfig(configFile),
                "Should throw when config file doesn't exist"
        );
        assertThat("Error message should be informative",
                exception.getMessage(), equalTo("src/test/resources/nonexistent-config.yaml"));
    }

    @Test
    void testReadConfigMalformedYamlException() {
        var configFile = Paths.get("src/test/resources/malformed-config.yaml");
        var exception = assertThrows(
                Exception.class,
                () -> EnumBiMapGenerator.readConfig(configFile),
                "Should throw when config file is malformed"
        );
        assertThat("Error message should contain path to file",
                exception.getMessage(), containsString("src/test/resources/malformed-config.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 5: readYamlData() - YAML Data Reading and Navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testReadYamlDataSimpleRootKey() throws IOException {
        var yamlFile = Paths.get("src/test/resources/valid-data.yaml");
        var rootKeys = List.of("data");

        var result = EnumBiMapGenerator.readYamlData(yamlFile, rootKeys);

        assertThat("Should return map with keys",
                result.keySet(), hasItems("key1", "key2"));
        assertThat("Should preserve list values",
                result.get("key1"), contains("value1", "value2"));
        assertThat("Should preserve list values",
                result.get("key2"), contains("value3"));
    }

    @Test
    void testReadYamlDataMultiLevelRootKeys() throws IOException {
        var yamlFile = Paths.get("src/test/resources/nested-data.yaml");
        var rootKeys = List.of("level1", "level2");

        var result = EnumBiMapGenerator.readYamlData(yamlFile, rootKeys);

        assertThat("Should navigate nested structure",
                result.keySet(), hasItems("nestedKey1", "nestedKey2"));
        assertThat("Should extract values from nested level",
                result.get("nestedKey1"), contains("nestedValue1"));
        assertThat("Should extract values from nested level",
                result.get("nestedKey2"), contains("nestedValue2"));
    }

    @Test
    void testReadYamlDataMissingFirstRootKeyThrows() throws IOException {
        var yamlFile = Paths.get("src/test/resources/valid-data.yaml");
        var rootKeys = List.of("missing");

        var exception = assertThrows(
                IllegalStateException.class,
                () -> EnumBiMapGenerator.readYamlData(yamlFile, rootKeys),
                "Should throw when first root-key missing"
        );
        assertThat("Error should mention missing root-key",
                exception.getMessage(), containsString("missing root-key"));
    }

    @Test
    void testReadYamlDataMissingNestedRootKeyThrows() throws IOException {
        var yamlFile = Paths.get("src/test/resources/nested-data.yaml");
        var rootKeys = List.of("level1", "missing");

        var exception = assertThrows(
                IllegalStateException.class,
                () -> EnumBiMapGenerator.readYamlData(yamlFile, rootKeys),
                "Should throw when nested root-key missing"
        );

        assertThat("Error should mention missing root-key",
                exception.getMessage(), containsString("missing root-keys"));
    }

    @Test
    void testReadYamlDataPreservesInsertionOrder() throws IOException {
        var yamlFile = Paths.get("src/test/resources/valid-data.yaml");
        var rootKeys = List.of("data");

        var result = EnumBiMapGenerator.readYamlData(yamlFile, rootKeys);

        // LinkedHashMap should preserve order
        var keys = new ArrayList<>(result.keySet());
        assertThat("Should maintain insertion order",
                keys, contains("key1", "key2"));
    }

    @Test
    void testReadYamlDataMissingFileThrows() {
        var yamlFile = Paths.get("src/test/resources/non-existent.yaml");
        var rootKeys = List.of("data");

        var exception = assertThrows(
                IOException.class,
                () -> EnumBiMapGenerator.readYamlData(yamlFile, rootKeys),
                "Should throw when YAML file doesn't exist"
        );
        assertThat("Error should include path to missing file",
                exception.getMessage(), Matchers.equalTo("src/test/resources/non-existent.yaml"));
    }

    @Test
    void testReadYamlDataMalformedYamlThrows() {
        var yamlFile = Paths.get("src/test/resources/malformed.yaml");
        var rootKeys = List.of("data");

        var exception = assertThrows(
                Exception.class,
                () -> EnumBiMapGenerator.readYamlData(yamlFile, rootKeys),
                "Should throw on malformed data YAML"
        );
        assertThat("Error should return an informative exception",
                exception.getMessage(), containsString("could not find expected"));
    }
}


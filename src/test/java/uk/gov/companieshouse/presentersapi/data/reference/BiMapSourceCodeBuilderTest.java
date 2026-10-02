package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BiMapSourceCodeBuilderTest {

    @Test
    void testValuesPerKeySetGeneratesCorrectFieldDeclarations() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.gov.companieshouse.data.bimaps",
                "TestFormGroupFormTypeBiMap",
                "FormGroup",
                "FormType",
                "formGroup",
                "by-form-group",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        // Forward mapping: ONE-TO-MANY
        assertThat("Should have forward mapping (FormGroup -> EnumSet<FormType>)",
                result, containsString("private final EnumMap<FormGroup, EnumSet<FormType>> byFormGroup;"));

        // Reverse mapping: SINGULAR (not a set)
        assertThat("Should have singular reverse mapping (FormType -> FormGroup)",
                result, containsString("private final EnumMap<FormType, FormGroup> byFormType;"));
    }

    @Test
    void testValuesPerKeySetConstructorParameterFromYamlProperty() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.gov.companieshouse.data.bimaps",
                "TestFormGroupFormTypeBiMap",
                "FormGroup",
                "FormType",
                "formGroup",
                "by-form-group",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        // YAML property "by-form-group" should be converted to camelCase "byFormGroup"
        assertThat("Constructor parameter should convert YAML property to camelCase",
                result, containsString("public TestFormGroupFormTypeBiMap(final Map<FormGroup, Set<FormType>> byFormGroup)"));
    }

    @Test
    void testValuesPerKeySetUsesPutIfAbsentForDuplicateDetection() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.gov.companieshouse.data.bimaps",
                "TestFormGroupFormTypeBiMap",
                "FormGroup",
                "FormType",
                "formGroup",
                "by-form-group",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        // Must use putIfAbsent() for singular variant
        assertThat("Should use putIfAbsent() for duplicate detection",
                result, containsString("reverseMapping.putIfAbsent(value, key)"));

        // Should throw exception on duplicate
        assertThat("Should throw IllegalStateException on duplicate value",
                result, containsString("throw new IllegalStateException("));
        assertThat("Exception message should indicate value mapped to multiple keys",
                result, containsString("mapped by both"));
    }

    @Test
    void testValuesPerKeySetReverseMethodIsSingular() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.gov.companieshouse.data.bimaps",
                "TestFormGroupFormTypeBiMap",
                "FormGroup",
                "FormType",
                "formGroup",
                "by-form-group",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        // Reverse method should be singular: formGroupFor(value) returns FormGroup
        assertThat("Reverse method should be singular (formGroupFor)",
                result, containsString("public FormGroup formGroupFor(final FormType value)"));

        // Should not have plural version
        assertThat("Should not have plural reverse method",
                result, not(containsString("public EnumSet<FormGroup> formGroupsFor")));
    }

    @Test
    void testValuesPerKeyGeneratesCorrectFieldDeclarations() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestKeyEnumValueEnumBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY
        );

        // Forward mapping: ONE-TO-MANY
        assertThat("Should have forward mapping (KeyEnum -> EnumSet<ValueEnum>)",
                result, containsString("private final EnumMap<KeyEnum, EnumSet<ValueEnum>> byKeyEnum;"));

        // Reverse mapping: PLURAL (is a set)
        assertThat("Should have plural reverse mapping (ValueEnum -> EnumSet<KeyEnum>)",
                result, containsString("private final EnumMap<ValueEnum, EnumSet<KeyEnum>> byValueEnum;"));
    }

    @Test
    void testValuesPerKeyConstructorParameterFromYamlProperty() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestKeyEnumValueEnumBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY
        );

        // YAML property "test-property" should be converted to camelCase "testProperty"
        assertThat("Constructor parameter should convert YAML property to camelCase",
                result, containsString("public TestKeyEnumValueEnumBiMap(final Map<KeyEnum, Set<ValueEnum>> testProperty)"));
    }

    @Test
    void testValuesPerKeyUsesComputeIfAbsentForAccumulation() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestKeyEnumValueEnumBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY
        );

        // Must use computeIfAbsent() for plural variant
        assertThat("Should use computeIfAbsent() to accumulate values",
                result, containsString("reverseMapping.computeIfAbsent(value, k -> EnumSet.noneOf(KeyEnum.class)).add(key)"));

        // Should NOT use putIfAbsent (that's for singular)
        assertThat("Should not use putIfAbsent in plural variant",
                result, not(containsString("reverseMapping.putIfAbsent(value")));

        // Should NOT throw exception for duplicates
        assertThat("Should not throw exception for duplicate values",
                result, not(containsString("throw new IllegalStateException(")));
    }

    @Test
    void testValuesPerKeyReverseMethodIsPlural() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestKeyEnumValueEnumBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY
        );

        // Reverse method should be plural: keyEnumsFor(value) returns EnumSet
        assertThat("Reverse method should be plural (keyEnumsFor)",
                result, containsString("public EnumSet<KeyEnum> keyEnumsFor(final ValueEnum value)"));

        // Should not have singular version
        assertThat("Should not have singular reverse method",
                result, not(containsString("public KeyEnum keyEnumFor")));
    }

    @Test
    void testBothVariantsHaveSameForwardMethod() {
        var resultSingular = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        var resultPlural = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY
        );

        // Forward method should be identical in both variants
        assertThat("Both variants should have same forward method",
                resultSingular, containsString("public EnumSet<ValueEnum> valueEnumsFor(final KeyEnum key)"));
        assertThat("Both variants should have same forward method",
                resultPlural, containsString("public EnumSet<ValueEnum> valueEnumsFor(final KeyEnum key)"));
    }

    @ParameterizedTest
    @CsvSource({
            "by-form-group,byFormGroup",
            "form-type-mapping,formTypeMapping",
            "presenter-type,presenterType",
            "single,single"
    })
    void testYamlPropertyConvertedToCamelCase(String yamlProperty, String expectedCamelCase) {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                yamlProperty,
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        assertThat("YAML property should be converted to camelCase in constructor",
                result, containsString("Map<KeyEnum, Set<ValueEnum>> " + expectedCamelCase + ")"));
    }

    @ParameterizedTest
    @CsvSource({
            "uk.gov.companieshouse.data.bimaps,uk.gov.companieshouse.data.enums",
            "uk.data.bimaps,uk.data.enums",
            "com.example.api.bimap,com.example.api.enums"
    })
    void testEnumPackageTransformation(String biMapPackage, String expectedEnumPackage) {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                biMapPackage,
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        assertThat("Enum imports should use correct enums package",
                result, containsString("import " + expectedEnumPackage + ".KeyEnum;"));
        assertThat("Enum imports should use correct enums package",
                result, containsString("import " + expectedEnumPackage + ".ValueEnum;"));
    }

    @Test
    void testConfigurationPropertiesAnnotation() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "myPrefix",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        assertThat("Should have @ConfigurationProperties with correct prefix",
                result, containsString("@ConfigurationProperties(prefix = \"myPrefix\")"));
    }

    @Test
    void testEmptyValueSetHandling() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        // Should handle empty sets with EnumSet.noneOf()
        assertThat("Should use EnumSet.noneOf() for empty value sets",
                result, containsString("EnumSet.noneOf(ValueEnum.class)"));
    }

    @Test
    void testGetterMethodsSafeReturn() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        // Getter methods should use getOrDefault() to avoid nulls
        assertThat("Forward getter should use getOrDefault for safe retrieval",
                result, containsString(".getOrDefault(key, EnumSet.noneOf(ValueEnum.class))"));
    }

    @Test
    void testNullUniquenessValidationDefaultsToValuesPerKey() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                null  // null -> should default to VALUES_PER_KEY
        );

        // Should generate plural reverse mapping (VALUES_PER_KEY behavior)
        assertThat("Null validation should default to VALUES_PER_KEY",
                result, containsString("EnumMap<ValueEnum, EnumSet<KeyEnum>>"));
        assertThat("Should use computeIfAbsent when defaulting to VALUES_PER_KEY",
                result, containsString("reverseMapping.computeIfAbsent(value, k -> EnumSet.noneOf(KeyEnum.class)).add(key)"));
    }

    @Test
    void testRequiredImports() {
        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        assertThat("Should import EnumMap", result, containsString("import java.util.EnumMap;"));
        assertThat("Should import EnumSet", result, containsString("import java.util.EnumSet;"));
        assertThat("Should import Set", result, containsString("import java.util.Set;"));
        assertThat("Should import Map", result, containsString("import java.util.Map;"));
        assertThat("Should import ConfigurationProperties",
                result, containsString("import org.springframework.boot.context.properties.ConfigurationProperties;"));
    }

    @Test
    void testClassNameGeneration() {

        var result = BiMapSourceCodeBuilder.buildBiMapSource(
                "uk.data.bimaps",
                "TestBiMap",
                "KeyEnum",
                "ValueEnum",
                "testKey",
                "test-property",
                EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET
        );

        assertThat("Should generate correct class declaration",
                result, containsString("public class TestBiMap {"));
        assertThat("Constructor should match class name",
                result, containsString("public TestBiMap("));
    }
}
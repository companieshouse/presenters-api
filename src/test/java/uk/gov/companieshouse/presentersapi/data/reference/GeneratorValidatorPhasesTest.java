package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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

/**
 * Unit test suite for the file-based validation phases of GeneratorValidator (phases 2-5).
 *
 * <p>Uses two sources in a temporary directory:
 * <ul>
 *   <li>keys.yaml: Group (dictionary keys) to Item (dictionary values); Group is validated as keys in other sources
 *   <li>other.yaml: Group (keys, via dictionary-keys-reference) to Other (dictionary values)
 * </ul>
 */
class GeneratorValidatorPhasesTest {

    private static final String KEYS_YAML = """
        m:
          by-group:
            g1:
              - i1
            g2:
              - i2
        """;

    private static final String OTHER_YAML = """
        o:
          by-group:
            g1:
              - o1
        """;

    @TempDir
    Path tempDir;

    private Map<String, GeneratedEnumDef> generatedEnums;

    private GeneratorValidator validator(final String keysYaml, final String otherYaml) throws IOException {
        return validator(keysYaml, otherYaml, validEnums());
    }

    private GeneratorValidator validator(
            final String keysYaml, final String otherYaml, final Map<String, Set<String>> enumValues) throws IOException {
        if (keysYaml != null) {
            Files.writeString(tempDir.resolve("keys.yaml"), keysYaml);
        }
        if (otherYaml != null) {
            Files.writeString(tempDir.resolve("other.yaml"), otherYaml);
        }

        final var keysSource = new EnumGeneratorConfig.Source(
            List.of("m", "by-group"), "Group", "Item",
            Map.of(
                "Group", new EnumGeneratorConfig.EnumDefinition(
                    EnumGeneratorConfig.Location.DICTIONARY_KEYS, null,
                    new EnumGeneratorConfig.EnumValidation(null, true)),
                "Item", new EnumGeneratorConfig.EnumDefinition(
                    EnumGeneratorConfig.Location.DICTIONARY_VALUES, null, null)));
        final var otherSource = new EnumGeneratorConfig.Source(
            List.of("o", "by-group"), "Group", "Other",
            Map.of(
                "Other", new EnumGeneratorConfig.EnumDefinition(
                    EnumGeneratorConfig.Location.DICTIONARY_VALUES, "Group", null)));

        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("keys.yaml", keysSource);
        sources.put("other.yaml", otherSource);
        final var config = new EnumGeneratorConfig(
            new EnumGeneratorConfig.OutputPackage("uk.test.enums", "uk.test.bimaps"), sources);

        generatedEnums = new LinkedHashMap<>();
        enumValues.forEach((name, values) ->
            generatedEnums.put(name, new GeneratedEnumDef(name, values, "uk.test.enums")));
        return new GeneratorValidator(config, tempDir, generatedEnums);
    }

    private static Map<String, Set<String>> validEnums() {
        final var enums = new LinkedHashMap<String, Set<String>>();
        enums.put("Group", new LinkedHashSet<>(List.of("g1", "g2")));
        enums.put("Item", new LinkedHashSet<>(List.of("i1", "i2")));
        enums.put("Other", new LinkedHashSet<>(List.of("o1")));
        return enums;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2: validateNoKeyValueConflictsWithinSources()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNoKeyValueConflictsWithinSourcesPassesForDistinctKeysAndValues() throws IOException {
        final var validator = validator(KEYS_YAML, OTHER_YAML);

        assertDoesNotThrow(validator::validateNoKeyValueConflictsWithinSources);
    }

    @Test
    void testNoKeyValueConflictsWithinSourcesThrowsWhenKeyIsAlsoAValue() throws IOException {
        final var validator = validator("""
            m:
              by-group:
                g1:
                  - g2
                g2:
                  - i2
            """, OTHER_YAML);

        final var exception = assertThrows(
            IllegalStateException.class, validator::validateNoKeyValueConflictsWithinSources);

        assertThat(exception.getMessage(), containsString("keys.yaml"));
        assertThat(exception.getMessage(), containsString("'g2' is used as both a mapping key and a mapping value"));
    }

    @Test
    void testNoKeyValueConflictsWithinSourcesPropagatesIoException() throws IOException {
        final var validator = validator(KEYS_YAML, null);

        assertThrows(NoSuchFileException.class, validator::validateNoKeyValueConflictsWithinSources);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3: validateReferencedKeysInOtherSources()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testReferencedKeysInOtherSourcesPassesWhenKeysAreValidEnumValues() throws IOException {
        final var validator = validator(KEYS_YAML, OTHER_YAML);

        assertDoesNotThrow(validator::validateReferencedKeysInOtherSources);
    }

    @Test
    void testReferencedKeysInOtherSourcesThrowsWhenKeyIsNotAValidEnumValue() throws IOException {
        final var validator = validator(KEYS_YAML, """
            o:
              by-group:
                unknown-group:
                  - o1
            """);

        final var exception = assertThrows(
            IllegalStateException.class, validator::validateReferencedKeysInOtherSources);

        assertThat(exception.getMessage(), containsString("invalid key(s)"));
        assertThat(exception.getMessage(), containsString("not valid Group"));
        assertThat(exception.getMessage(), containsString("unknown-group"));
        assertThat("Hint about values defined elsewhere should be shown for key validation",
            exception.getMessage(), containsString("Hint"));
    }

    @Test
    void testReferencedKeysInOtherSourcesPropagatesIoException() throws IOException {
        final var validator = validator(KEYS_YAML, null);

        assertThrows(NoSuchFileException.class, validator::validateReferencedKeysInOtherSources);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 4: validateNoValueConflictsAcrossRoles()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNoValueConflictsAcrossRolesPassesWhenRolesAreDistinct() throws IOException {
        final var validator = validator(KEYS_YAML, OTHER_YAML);

        assertDoesNotThrow(validator::validateNoValueConflictsAcrossRoles);
    }

    @Test
    void testNoValueConflictsAcrossRolesThrowsWhenKeyOfOneEnumIsValueOfAnother() throws IOException {
        // g2 is a key of Group in keys.yaml, and a value of Other in other.yaml
        final var validator = validator(KEYS_YAML, """
            o:
              by-group:
                g1:
                  - g2
            """);

        final var exception = assertThrows(
            IllegalStateException.class, validator::validateNoValueConflictsAcrossRoles);

        assertThat(exception.getMessage(), containsString("'g2' cannot be used as both a mapping key and mapping value"));
        assertThat(exception.getMessage(), containsString("key for enum: Group"));
        assertThat(exception.getMessage(), containsString("value for enum: Other"));
    }

    @Test
    void testNoValueConflictsAcrossRolesPropagatesIoException() throws IOException {
        final var validator = validator(KEYS_YAML, null);

        assertThrows(NoSuchFileException.class, validator::validateNoValueConflictsAcrossRoles);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 5: validateNoForeignEnumValuesInSources()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNoForeignEnumValuesPassesWhenTokensMatchTheirRoles() throws IOException {
        final var validator = validator(KEYS_YAML, OTHER_YAML);

        assertDoesNotThrow(validator::validateNoForeignEnumValuesInSources);
    }

    @Test
    void testNoForeignEnumValuesThrowsWhenKeyBelongsToAnotherEnum() throws IOException {
        // o1 is owned by Other, but other.yaml uses it as a dictionary key (expects Group)
        final var validator = validator(KEYS_YAML, """
            o:
              by-group:
                o1:
                  - o1
            """);

        final var exception = assertThrows(
            IllegalStateException.class, validator::validateNoForeignEnumValuesInSources);

        assertThat(exception.getMessage(), containsString("foreign enum token in DICTIONARY_KEY"));
        assertThat(exception.getMessage(), containsString("Token 'o1' belongs to enum 'Other'"));
        assertThat(exception.getMessage(), containsString("only allows enums: [Group]"));
    }

    @Test
    void testNoForeignEnumValuesThrowsWhenValueBelongsToAnotherEnum() throws IOException {
        // i1 is owned by Item, but other.yaml uses it as a dictionary value (expects Other)
        final var validator = validator(KEYS_YAML, """
            o:
              by-group:
                g1:
                  - i1
            """);

        final var exception = assertThrows(
            IllegalStateException.class, validator::validateNoForeignEnumValuesInSources);

        assertThat(exception.getMessage(), containsString("foreign enum token in DICTIONARY_VALUE"));
        assertThat(exception.getMessage(), containsString("Token 'i1' belongs to enum 'Item'"));
    }

    @Test
    void testNoForeignEnumValuesThrowsWhenTokenIsNotInAnyEnum() throws IOException {
        final var validator = validator(KEYS_YAML, """
            o:
              by-group:
                g1:
                  - mystery
            """);

        final var exception = assertThrows(
            IllegalStateException.class, validator::validateNoForeignEnumValuesInSources);

        assertThat(exception.getMessage(), containsString("unknown enum token in DICTIONARY_VALUE"));
        assertThat(exception.getMessage(), containsString("'mystery' does not belong to any known enum type"));
    }

    @Test
    void testNoForeignEnumValuesThrowsWhenValueIsOwnedByMultipleEnums() throws IOException {
        final var enums = validEnums();
        enums.get("Other").add("i1");
        final var validator = validator(KEYS_YAML, OTHER_YAML, enums);

        final var exception = assertThrows(
            IllegalStateException.class, validator::validateNoForeignEnumValuesInSources);

        assertThat(exception.getMessage(), containsString("enum value 'i1' is defined in multiple enums"));
        assertThat(exception.getMessage(), containsString("First owner: 'Item'"));
        assertThat(exception.getMessage(), containsString("Second owner: 'Other'"));
    }

    @Test
    void testNoForeignEnumValuesAllowsSameEnumToOwnAValueRepeatedly() throws IOException {
        final var validator = validator(KEYS_YAML, OTHER_YAML);

        assertDoesNotThrow(validator::validateNoForeignEnumValuesInSources);
        assertThat("Validation must not alter the registered enum values",
            generatedEnums.get("Group").values(), is(Set.of("g1", "g2")));
    }

    @Test
    void testNoForeignEnumValuesPropagatesIoException() throws IOException {
        final var validator = validator(KEYS_YAML, null);

        assertThrows(NoSuchFileException.class, validator::validateNoForeignEnumValuesInSources);
    }

    @Test
    void testNoForeignEnumValuesRejectsAnyTokenWhenSourceHasNoKeyOrValueEnum() throws IOException {
        Files.writeString(tempDir.resolve("bare.yaml"), "b:\n  by-group:\n    g1:\n      - i1\n");
        final var bare = new EnumGeneratorConfig.Source(List.of("b", "by-group"), null, null, Map.of());
        final var config = new EnumGeneratorConfig(
            new EnumGeneratorConfig.OutputPackage("uk.test.enums", "uk.test.bimaps"),
            Map.of("bare.yaml", bare));
        final var enums = new LinkedHashMap<String, GeneratedEnumDef>();
        enums.put("Group", new GeneratedEnumDef("Group", Set.of("g1"), "uk.test.enums"));
        enums.put("Item", new GeneratedEnumDef("Item", Set.of("i1"), "uk.test.enums"));
        final var validator = new GeneratorValidator(config, tempDir, enums);

        final var exception = assertThrows(
            IllegalStateException.class, validator::validateNoForeignEnumValuesInSources);

        assertThat(exception.getMessage(), containsString("only allows enums: []"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Branch coverage: configuration variations
    // ─────────────────────────────────────────────────────────────────────────

    private GeneratorValidator validatorFor(
            final Map<String, EnumGeneratorConfig.Source> sources, final Map<String, Set<String>> enumValues) {
        final var config = new EnumGeneratorConfig(
            new EnumGeneratorConfig.OutputPackage("uk.test.enums", "uk.test.bimaps"), sources);
        final var enums = new LinkedHashMap<String, GeneratedEnumDef>();
        enumValues.forEach((name, values) -> enums.put(name, new GeneratedEnumDef(name, values, "uk.test.enums")));
        return new GeneratorValidator(config, tempDir, enums);
    }

    private static EnumGeneratorConfig.Source valuesOnlySource(
            final String rootKey, final String enumName, final EnumGeneratorConfig.EnumValidation validation) {
        return new EnumGeneratorConfig.Source(
            List.of(rootKey, "by-group"), null, null,
            Map.of(enumName, new EnumGeneratorConfig.EnumDefinition(
                EnumGeneratorConfig.Location.DICTIONARY_VALUES, null, validation)));
    }

    @Test
    void testReferencedKeysInOtherSourcesPassesWhenNoOtherSourceReferencesTheEnum() throws IOException {
        Files.writeString(tempDir.resolve("keys.yaml"), KEYS_YAML);
        final var keysSource = new EnumGeneratorConfig.Source(
            List.of("m", "by-group"), "Group", "Item",
            Map.of("Group", new EnumGeneratorConfig.EnumDefinition(
                EnumGeneratorConfig.Location.DICTIONARY_KEYS, null,
                new EnumGeneratorConfig.EnumValidation(null, true))));
        final var validator = validatorFor(Map.of("keys.yaml", keysSource), validEnums());

        assertDoesNotThrow(validator::validateReferencedKeysInOtherSources);
    }

    @Test
    void testReferencedKeysInOtherSourcesIgnoresEnumsNotFlaggedForKeyValidation() {
        // Group is not registered in generatedEnums: it would fail if any of these sources were selected
        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("no-validation.yaml", valuesOnlySource("a", "Group", null));
        sources.put("null-flag.yaml", valuesOnlySource("b", "Group", new EnumGeneratorConfig.EnumValidation(null, null)));
        sources.put("false-flag.yaml", valuesOnlySource("c", "Group", new EnumGeneratorConfig.EnumValidation(null, false)));
        final var validator = validatorFor(sources, Map.of());

        assertDoesNotThrow(validator::validateReferencedKeysInOtherSources);
    }

    @Test
    void testNoValueConflictsAcrossRolesAllowsSameEnumAsKeyAndValue() throws IOException {
        Files.writeString(tempDir.resolve("keys.yaml"), KEYS_YAML);
        Files.writeString(tempDir.resolve("same.yaml"), "s:\n  by-group:\n    x:\n      - g1\n");
        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("keys.yaml", new EnumGeneratorConfig.Source(
            List.of("m", "by-group"), "Group", "Item",
            Map.of("Group", new EnumGeneratorConfig.EnumDefinition(
                EnumGeneratorConfig.Location.DICTIONARY_KEYS, null, null))));
        // g1 is a key of Group in keys.yaml and a value of the same enum Group here
        sources.put("same.yaml", valuesOnlySource("s", "Group", null));
        final var validator = validatorFor(sources, Map.of());

        assertDoesNotThrow(validator::validateNoValueConflictsAcrossRoles);
    }

    @Test
    void testValidateDictionaryKeysMatchOmitsHintForValueValidation() throws Exception {
        final var method = GeneratorValidator.class.getDeclaredMethod(
            "validateDictionaryKeysMatch", Map.class, Set.class, String.class, Path.class, boolean.class);
        method.setAccessible(true);
        final Map<String, Set<String>> yamlData = Map.of("bad", Set.of("x"));

        final var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(null, yamlData, Set.of("good"), "Group", tempDir.resolve("f.yaml"), false));

        final var message = thrown.getCause().getMessage();
        assertThat(message, containsString("invalid value(s)"));
        assertThat("Hint is only relevant to key validation", message.contains("Hint"), is(false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WrappedIOException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testWrappedIoExceptionUnwrapsToOriginalCause() {
        final var cause = new IOException("disk failure");

        final var unwrapped = new GeneratorValidator.WrappedIOException(cause).unwrap();

        assertThat(unwrapped, is(cause));
    }
}

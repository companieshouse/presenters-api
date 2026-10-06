package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit test suite for the EnumBiMapGenerator entry point (main) and its full generation pipeline.
 * Tests argument handling, config directory resolution, successful end-to-end generation, and
 * failures raised by each pipeline phase.
 */
class EnumBiMapGeneratorTest {

    private static final String CONFIG = """
        output-package:
          enums: uk.test.enums
          bimaps: uk.test.bimaps
        sources:
          groups.yaml:
            root-keys: [groups-map, by-group]
            key-enum: Group
            value-enum: Item
            enums:
              Group:
                location: DICTIONARY_KEYS
                validation:
                  uniqueness: VALUES_PER_KEY_SET
                  validate-as-keys-in-other-sources: true
              Item:
                location: DICTIONARY_VALUES
                validation:
                  uniqueness: VALUES_PER_KEY_SET
          others.yaml:
            root-keys: [others-map, by-group]
            key-enum: Group
            value-enum: Other
            enums:
              Other:
                location: DICTIONARY_VALUES
                dictionary-keys-reference: Group
                validation:
                  uniqueness: VALUES_PER_KEY
        """;

    private static final String GROUPS_YAML = """
        groups-map:
          by-group:
            g1:
              - i1
            g2:
              - i2
        """;

    private static final String OTHERS_YAML = """
        others-map:
          by-group:
            g1:
              - o1
        """;

    @TempDir
    Path tempDir;

    private Path out() {
        return tempDir.resolve("out");
    }

    private String[] args() {
        return new String[] {tempDir.resolve("config.yaml").toString(), out().toString()};
    }

    private void writeInputs(final String groupsYaml, final String othersYaml) throws IOException {
        Files.writeString(tempDir.resolve("config.yaml"), CONFIG);
        Files.writeString(tempDir.resolve("groups.yaml"), groupsYaml);
        Files.writeString(tempDir.resolve("others.yaml"), othersYaml);
    }

    @Test
    void testMainGeneratesEnumsAndBiMapsFromConfig() throws Exception {
        writeInputs(GROUPS_YAML, OTHERS_YAML);

        EnumBiMapGenerator.main(args());

        assertThat(Files.readString(out().resolve("uk/test/enums/Group.java")), containsString("G1(\"g1\")"));
        assertThat(Files.readString(out().resolve("uk/test/enums/Item.java")), containsString("I2(\"i2\")"));
        assertThat(Files.readString(out().resolve("uk/test/enums/Other.java")), containsString("O1(\"o1\")"));
        assertThat(Files.readString(out().resolve("uk/test/bimaps/GroupItemBiMap.java")),
            containsString("EnumMap<Item, Group> byItem"));
        assertThat(Files.readString(out().resolve("uk/test/bimaps/GroupOtherBiMap.java")),
            containsString("EnumMap<Other, EnumSet<Group>> byOther"));
    }

    @Test
    void testMainResolvesSourcesRelativeToConfigFileDirectory() throws Exception {
        final var configDir = Files.createDirectory(tempDir.resolve("conf"));
        Files.writeString(configDir.resolve("config.yaml"), CONFIG);
        Files.writeString(configDir.resolve("groups.yaml"), GROUPS_YAML);
        Files.writeString(configDir.resolve("others.yaml"), OTHERS_YAML);

        EnumBiMapGenerator.main(new String[] {configDir.resolve("config.yaml").toString(), out().toString()});

        assertThat(Files.exists(out().resolve("uk/test/enums/Group.java")), is(true));
    }

    @Test
    void testMainWithBareConfigFileNameResolvesAgainstCurrentDirectory() {
        // A config path without a parent falls back to "."; the file does not exist, so reading it fails
        assertThrows(NoSuchFileException.class,
            () -> EnumBiMapGenerator.main(new String[] {"no-such-config-file.yaml", out().toString()}));
    }

    @Test
    void testMainRejectsTooFewArguments() {
        final var exception = assertThrows(IllegalArgumentException.class,
            () -> EnumBiMapGenerator.main(new String[] {"config.yaml"}));

        assertThat(exception.getMessage(), containsString("Usage: <configFile> <outputDir>"));
    }

    @Test
    void testMainRejectsTooManyArguments() {
        assertThrows(IllegalArgumentException.class,
            () -> EnumBiMapGenerator.main(new String[] {"config.yaml", "out", "extra"}));
    }

    @Test
    void testMainRejectsNoArguments() {
        assertThrows(IllegalArgumentException.class, () -> EnumBiMapGenerator.main(new String[] {}));
    }

    @Test
    void testMainFailsWhenConfigFileIsMissing() {
        final var arguments = args();
        assertThrows(NoSuchFileException.class, () -> EnumBiMapGenerator.main(arguments));
    }

    @Test
    void testMainFailsWhenKeyAlsoAppearsAsValueWithinSource() throws Exception {
        writeInputs("""
            groups-map:
              by-group:
                g1:
                  - g2
                g2:
                  - i2
            """, OTHERS_YAML);

        final var arguments = args();

        final var exception = assertThrows(IllegalStateException.class, () -> EnumBiMapGenerator.main(arguments));

        assertThat(exception.getMessage(), containsString("used as both a mapping key and a mapping value"));
    }

    @Test
    void testMainFailsWhenReferencedKeyIsNotAValidEnumValue() throws Exception {
        writeInputs(GROUPS_YAML, """
            others-map:
              by-group:
                unknown:
                  - o1
            """);

        final var arguments = args();

        final var exception = assertThrows(IllegalStateException.class, () -> EnumBiMapGenerator.main(arguments));

        assertThat(exception.getMessage(), containsString("not valid Group"));
    }

    @Test
    void testMainFailsWhenValueIsOwnedByAnotherEnum() throws Exception {
        writeInputs(GROUPS_YAML, """
            others-map:
              by-group:
                g1:
                  - i1
            """);

        final var arguments = args();

        final var exception = assertThrows(IllegalStateException.class, () -> EnumBiMapGenerator.main(arguments));

        assertThat(exception.getMessage(), containsString("defined in multiple enums"));
    }

    @Test
    void testMainFailsWhenSourceFileIsMissing() throws Exception {
        writeInputs(GROUPS_YAML, OTHERS_YAML);
        Files.delete(tempDir.resolve("others.yaml"));

        final var arguments = args();

        assertThrows(NoSuchFileException.class, () -> EnumBiMapGenerator.main(arguments));
    }
}

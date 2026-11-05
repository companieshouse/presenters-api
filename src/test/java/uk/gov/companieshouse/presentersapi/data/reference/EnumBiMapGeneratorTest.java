package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldFailBeforeBiMapGenerationWhenCoverageIsMissing(final boolean wrapResources) throws Exception {
        writeInputs(wrapResources ? "g1: i1\ng2: [i2]\n" : GROUPS_YAML,
            wrapResources ? "g1: o1\n" : OTHERS_YAML);
        Files.writeString(tempDir.resolve("config.yaml"), CONFIG.replace(
            "    value-enum: Item", """
                value-enum: Item
                require-non-empty-values-in:
                  - target: others.yaml""".indent(4).stripTrailing()));

        final var arguments = wrapResources
            ? new String[] {args()[0], out().toString(), tempDir.resolve("resources").toString()} : args();
        final var exception = assertThrows(IllegalStateException.class, () -> EnumBiMapGenerator.main(arguments));

        assertThat(exception.getMessage(), containsString("groups.yaml"));
        assertThat(exception.getMessage(), containsString("others.yaml"));
        assertThat(exception.getMessage(), containsString("g2=[i2]"));
        assertThat(Files.exists(out().resolve("uk/test/bimaps/GroupItemBiMap.java")), is(false));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldGenerateBiMapsWhenMissingCoverageIsExcluded(final boolean wrapResources) throws Exception {
        writeInputs(wrapResources ? "g1: i1\ng2: [i2]\n" : GROUPS_YAML,
            wrapResources ? "g1: o1\n" : OTHERS_YAML);
        Files.writeString(tempDir.resolve("config.yaml"), CONFIG.replace(
            "    value-enum: Item", """
                value-enum: Item
                require-non-empty-values-in:
                  - target: others.yaml
                    excluded-keys: [g2]""".indent(4).stripTrailing()));

        final var arguments = wrapResources
            ? new String[] {args()[0], out().toString(), tempDir.resolve("resources").toString()} : args();
        EnumBiMapGenerator.main(arguments);

        assertThat(Files.exists(out().resolve("uk/test/bimaps/GroupItemBiMap.java")), is(true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldLogEachPhaseBeforeExecutingIt(final boolean failCoverage) throws Exception {
        writeInputs(GROUPS_YAML, OTHERS_YAML);
        if (failCoverage) {
            Files.writeString(tempDir.resolve("config.yaml"), CONFIG.replace("    value-enum: Item",
                "    value-enum: Item\n    require-non-empty-values-in:\n      - target: others.yaml"));
        }
        final var logger = (Logger) LoggerFactory.getLogger("uk.gov");
        final var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            final var args = args();
            if (failCoverage) {
                assertThrows(IllegalStateException.class, () -> EnumBiMapGenerator.main(args));
            } else {
                EnumBiMapGenerator.main(args);
            }
            final var phases = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("Phase ")).toList();
            final var expected = List.of(
                "Phase 1 of 7: Generate enums",
                "Phase 2 of 7: Validate key-value conflicts within sources",
                "Phase 3 of 7: Validate referenced keys in other sources",
                "Phase 4 of 7: Validate value conflicts across roles",
                "Phase 5 of 7: Validate enum value ownership",
                "Phase 6 of 7: Validate non-empty target mappings",
                "Phase 7 of 7: Generate BiMaps");
            assertThat(phases, is(failCoverage ? expected.subList(0, 6) : expected));
            assertThat(appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains("[Phase 7 summary]")), is(false));
            final var summaries = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("  [Generated] BiMap")).toList();
            assertThat(summaries, is(failCoverage ? List.of() : List.of(
                "  [Generated] BiMap 'GroupItemBiMap' for Group (2 keys) ↔ Item (2 values)",
                "  [Generated] BiMap 'GroupOtherBiMap' for Group (1 keys) ↔ Other (1 values)")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
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
    void testMainGeneratesResourcesBeforeEnumsAndBiMaps() throws Exception {
        writeInputs("g1: i1\ng2: [i2]\n", "g1: o1\n");
        final var resourcesDir = tempDir.resolve("resources");

        EnumBiMapGenerator.main(new String[] {args()[0], out().toString(), resourcesDir.toString()});

        assertThat(GeneratorData.readYamlData(
            resourcesDir.resolve("data/groups.yaml"), List.of("groups-map", "by-group")),
            is(Map.of("g1", List.of("i1"), "g2", List.of("i2"))));
        assertThat(Files.readString(out().resolve("uk/test/enums/Item.java")), containsString("I2(\"i2\")"));
        final var bimap = Files.readString(out().resolve("uk/test/bimaps/GroupItemBiMap.java"));
        assertThat(bimap, containsString("@ConfigurationProperties(prefix = \"groups-map\")"));
        assertThat(bimap, containsString("byGroup"));
    }

    @Test
    void testGeneratedResourcesStillUndergoUniquenessValidation() throws Exception {
        writeInputs("g1: i1\ng2: i1\n", "g1: o1\n");
        final var arguments = new String[] {args()[0], out().toString(), tempDir.resolve("resources").toString()};

        assertThrows(IllegalStateException.class, () -> EnumBiMapGenerator.main(arguments));
    }

    @Test
    void testGeneratedResourcesStillUndergoReferencedKeyValidation() throws Exception {
        writeInputs("g1: i1\ng2: i2\n", "unknown: o1\n");
        final var arguments = new String[] {args()[0], out().toString(), tempDir.resolve("resources").toString()};

        final var exception = assertThrows(IllegalStateException.class, () -> EnumBiMapGenerator.main(arguments));

        assertThat(exception.getMessage(), containsString("not valid Group"));
    }

    @Test
    void testGeneratedResourcesStillRejectInvalidEnumCodes() throws Exception {
        for (final var input : List.of("g1: [foo-bar, foo_bar]\n", "g1: [\"\"]\n")) {
            writeInputs(input, "g1: o1\n");
            final var arguments = new String[] {args()[0], out().toString(), tempDir.resolve("resources").toString()};

            assertThrows(IllegalStateException.class, () -> EnumBiMapGenerator.main(arguments));
        }
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
            () -> EnumBiMapGenerator.main(new String[] {"config.yaml", "out", "resources", "extra"}));
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

package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit test suite for BiMapGenerator.generateBiMapsFromSources().
 * Tests which sources produce a BiMap file and which reverse-mapping type is generated:
 * - Sources lacking a key-enum or value-enum are skipped
 * - VALUES_PER_KEY_SET produces a singular reverse mapping
 * - VALUES_PER_KEY (or no validation configured) produces a plural reverse mapping
 * - File system failures surface as IOException
 */
class BiMapGeneratorTest {

    private static final String BIMAP_PACKAGE = "uk.test.bimaps";

    @TempDir
    Path tempDir;

    private Path bimapFile(final String className) {
        return tempDir.resolve("uk/test/bimaps/" + className + ".java");
    }

    private static EnumGeneratorConfig.Source source(
            final String keyEnum, final String valueEnum, final Map<String, EnumGeneratorConfig.EnumDefinition> enums) {
        return new EnumGeneratorConfig.Source(List.of("root", "by-group"), keyEnum, valueEnum, enums);
    }

    private static Map<String, EnumGeneratorConfig.EnumDefinition> valueEnum(
            final EnumGeneratorConfig.EnumValidation validation) {
        return Map.of("Item", new EnumGeneratorConfig.EnumDefinition(
            EnumGeneratorConfig.Location.DICTIONARY_VALUES, null, validation));
    }

    private BiMapGenerator generatorFor(final Map<String, EnumGeneratorConfig.Source> sources, final Path outputDir) {
        final var config = new EnumGeneratorConfig(
            new EnumGeneratorConfig.OutputPackage("uk.test.enums", BIMAP_PACKAGE), sources);
        return new BiMapGenerator(config, outputDir);
    }

    @Test
    void testGeneratesBiMapNamedFromKeyAndValueEnums() throws IOException {
        final var generator = generatorFor(Map.of("a.yaml", source("Group", "Item", valueEnum(null))), tempDir);

        generator.generateBiMapsFromSources();

        final var content = Files.readString(bimapFile("GroupItemBiMap"));
        assertThat(content, containsString("package " + BIMAP_PACKAGE + ";"));
        assertThat(content, containsString("public class GroupItemBiMap"));
        assertThat("Prefix is the first root key", content, containsString("@ConfigurationProperties(prefix = \"root\")"));
        assertThat("Enums are imported from the enums package",
            content, containsString("import uk.test.enums.Item;"));
    }

    @Test
    void testValuesPerKeySetGeneratesSingularReverseMapping() throws IOException {
        final var validation = new EnumGeneratorConfig.EnumValidation(
            EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET, null);
        final var generator = generatorFor(Map.of("a.yaml", source("Group", "Item", valueEnum(validation))), tempDir);

        generator.generateBiMapsFromSources();

        final var content = Files.readString(bimapFile("GroupItemBiMap"));
        assertThat(content, containsString("EnumMap<Item, Group> byItem"));
    }

    @Test
    void testValuesPerKeyGeneratesPluralReverseMapping() throws IOException {
        final var validation = new EnumGeneratorConfig.EnumValidation(
            EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY, null);
        final var generator = generatorFor(Map.of("a.yaml", source("Group", "Item", valueEnum(validation))), tempDir);

        generator.generateBiMapsFromSources();

        final var content = Files.readString(bimapFile("GroupItemBiMap"));
        assertThat(content, containsString("EnumMap<Item, EnumSet<Group>> byItem"));
    }

    @Test
    void testNullValidationDefaultsToPluralReverseMapping() throws IOException {
        final var generator = generatorFor(Map.of("a.yaml", source("Group", "Item", valueEnum(null))), tempDir);

        generator.generateBiMapsFromSources();

        final var content = Files.readString(bimapFile("GroupItemBiMap"));
        assertThat(content, containsString("EnumMap<Item, EnumSet<Group>> byItem"));
    }

    @Test
    void testValidationWithoutUniquenessDefaultsToPluralReverseMapping() throws IOException {
        final var validation = new EnumGeneratorConfig.EnumValidation(null, true);
        final var generator = generatorFor(Map.of("a.yaml", source("Group", "Item", valueEnum(validation))), tempDir);

        generator.generateBiMapsFromSources();

        final var content = Files.readString(bimapFile("GroupItemBiMap"));
        assertThat(content, containsString("EnumMap<Item, EnumSet<Group>> byItem"));
    }

    @Test
    void testNoValueLocationEnumDefaultsToPluralReverseMapping() throws IOException {
        final var keysOnly = Map.of("Group", new EnumGeneratorConfig.EnumDefinition(
            EnumGeneratorConfig.Location.DICTIONARY_KEYS, null, null));
        final var generator = generatorFor(Map.of("a.yaml", source("Group", "Item", keysOnly)), tempDir);

        generator.generateBiMapsFromSources();

        final var content = Files.readString(bimapFile("GroupItemBiMap"));
        assertThat(content, containsString("EnumMap<Item, EnumSet<Group>> byItem"));
    }

    @Test
    void testSkipsSourceWithoutKeyEnum() throws IOException {
        final var generator = generatorFor(Map.of("a.yaml", source(null, "Item", valueEnum(null))), tempDir);

        generator.generateBiMapsFromSources();

        assertThat("No BiMap output expected", Files.exists(tempDir.resolve("uk")), is(false));
    }

    @Test
    void testSkipsSourceWithoutValueEnum() throws IOException {
        final var generator = generatorFor(Map.of("a.yaml", source("Group", null, valueEnum(null))), tempDir);

        generator.generateBiMapsFromSources();

        assertThat("No BiMap output expected", Files.exists(tempDir.resolve("uk")), is(false));
    }

    @Test
    void testGeneratesOneBiMapPerEligibleSourceOnly() throws IOException {
        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("a.yaml", source("Group", "Item", valueEnum(null)));
        sources.put("b.yaml", source(null, "Other", valueEnum(null)));
        sources.put("c.yaml", source("Owner", "Thing", valueEnum(null)));
        final var generator = generatorFor(sources, tempDir);

        generator.generateBiMapsFromSources();

        assertThat(Files.exists(bimapFile("GroupItemBiMap")), is(true));
        assertThat(Files.exists(bimapFile("OwnerThingBiMap")), is(true));
        assertThat(Files.exists(bimapFile("NullOtherBiMap")), is(false));
        try (var files = Files.list(bimapFile("GroupItemBiMap").getParent())) {
            assertThat(files.count(), is(2L));
        }
    }

    @Test
    void testNoSourcesGeneratesNothing() {
        final var generator = generatorFor(Map.of(), tempDir);

        assertDoesNotThrow(generator::generateBiMapsFromSources);
        assertThat(Files.exists(tempDir.resolve("uk")), is(false));
    }

    @Test
    void testOverwritesExistingBiMapFile() throws IOException {
        Files.createDirectories(bimapFile("GroupItemBiMap").getParent());
        Files.writeString(bimapFile("GroupItemBiMap"), "stale content");
        final var generator = generatorFor(Map.of("a.yaml", source("Group", "Item", valueEnum(null))), tempDir);

        generator.generateBiMapsFromSources();

        assertThat(Files.readString(bimapFile("GroupItemBiMap")), not(containsString("stale content")));
    }

    @Test
    void testPropagatesIoExceptionWhenOutputDirectoryIsAFile() throws IOException {
        final var notADirectory = Files.writeString(tempDir.resolve("blocker"), "file");
        final var generator = generatorFor(Map.of("a.yaml", source("Group", "Item", valueEnum(null))), notADirectory);

        assertThrows(IOException.class, generator::generateBiMapsFromSources);
    }
}

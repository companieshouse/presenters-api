package uk.gov.companieshouse.presentersapi.data.reference;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates runtime YAML resources, Java enum classes and bidirectional maps (BiMaps).
 *
 * <p>This class orchestrates the end-to-end process of reading YAML enum definitions, validating
 * data consistency and cardinality constraints, and generating Java enum and BiMap source files.
 * It supports complex bidirectional mapping scenarios where enums can appear as both dictionary
 * keys and values across multiple sources (e.g., FormGroup ↔ FormType, PresenterType ↔
 * DeliveryStatement).
 *
 * <p><b>Core Responsibilities:</b>
 * <ul>
 *   <li><b>Orchestration:</b> Coordinates EnumGenerator, BiMapGenerator, and GeneratorValidator
 *       to execute the multi-phase generation and validation pipeline.
 *   <li><b>Configuration Loading:</b> Uses GeneratorData to read enum-generator-config.yaml and determine which YAML
 *       sources contribute to which enums, along with their validation and cardinality rules.
 *   <li><b>Resource Generation:</b> Wraps documentation dictionaries in binding keys when a resources
 *       output directory is supplied.
 * </ul>
 *
 * <p><b>Delegation Model:</b>
 * <ul>
 *   <li><b>EnumGenerator:</b> Handles enum extraction, merging, registration, and enum file writing
 *   <li><b>BiMapGenerator:</b> Handles bimap class generation and bimap file writing
 *   <li><b>GeneratorValidator:</b> Handles all validation logic (cardinality, conflicts, ownership)
 * </ul>
 *
 * <p><b>Exception Handling:</b> This class and its delegates use {@link GeneratorValidator.WrappedIOException}
 * as a workaround for Java's stream API limitation: stream lambdas cannot throw checked exceptions.
 * Checked {@link IOException}s are wrapped in this unchecked exception inside stream operations, then
 * unwrapped and rethrown in surrounding try-catch blocks to maintain proper exception semantics
 * for callers.
 *
 * <p><b>Usage:</b>
 * <pre>
 *   final var generator = new EnumBiMapGenerator(config, configDir, outputDir);
 *   generator.generate();
 * </pre>
 */
public class EnumBiMapGenerator {

    private final EnumGenerator enumGenerator;
    private final BiMapGenerator biMapGenerator;
    private final GeneratorValidator validator;

    private EnumBiMapGenerator(final EnumGeneratorConfig config, final Path configDir, final Path outputDir) {
        this.enumGenerator = new EnumGenerator(config, configDir, outputDir);
        this.biMapGenerator = new BiMapGenerator(config, outputDir);
        this.validator = enumGenerator.getValidator();
    }

    public static void main(final String[] args) throws Exception {
        final var configFile = validateAndParseArgs(args);
        final var outputDir = Path.of(args[1]);
        final var configDir = configFile.getParent() != null ? configFile.getParent() : Path.of(".");

        final var config = GeneratorData.readConfig(configFile);
        final EnumBiMapGenerator generator;
        if (args.length == 3) {
            final var resourcesDir = Path.of(args[2]);
            final var generatedConfig = YamlResourceGenerator.generate(config, configDir, resourcesDir);
            generator = new EnumBiMapGenerator(generatedConfig, resourcesDir, outputDir);
        } else {
            generator = new EnumBiMapGenerator(config, configDir, outputDir);
        }
        generator.generate();
    }

    private void generate() throws IOException {
        enumGenerator.generateEnumsFromSources();
        validator.validateNoKeyValueConflictsWithinSources();
        validator.validateReferencedKeysInOtherSources();
        validator.validateNoValueConflictsAcrossRoles();
        validator.validateNoForeignEnumValuesInSources();
        biMapGenerator.generateBiMapsFromSources();
    }

    private static Path validateAndParseArgs(final String[] args) {
        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Usage: <configFile> <outputDir> [resourcesDir]");
        }
        return Path.of(args[0]);
    }

}

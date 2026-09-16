package uk.gov.companieshouse.presentersapi.data.reference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

/**
 * Generates Java enum classes and bidirectional maps (BiMaps) from YAML configuration.
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
 *   <li><b>Configuration Loading:</b> Reads enum-generator-config.yaml to determine which YAML
 *       sources contribute to which enums, along with their validation and cardinality rules.
 *   <li><b>YAML Data Reading:</b> Loads and navigates YAML structures based on configured root-keys.
 *   <li><b>Shared Utilities:</b> Provides common string escaping and configuration reading functions.
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

    // Static patterns for enum constant normalization (accessed by EnumGenerator)
    static final Pattern BACKSLASH = Pattern.compile("\\\\");
    static final Pattern QUOTE = Pattern.compile("\"");

    public static void main(final String[] args) throws Exception {
        final var configFile = validateAndParseArgs(args);
        final var outputDir = Path.of(args[1]);
        final var configDir = configFile.getParent() != null ? configFile.getParent() : Path.of(".");

        final var config = readConfig(configFile);
        final var generator = new EnumBiMapGenerator(config, configDir, outputDir);
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
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: <configFile> <outputDir>");
        }
        return Path.of(args[0]);
    }

    static EnumGeneratorConfig readConfig(final Path configFile) throws IOException {
        final var mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(Files.readString(configFile), EnumGeneratorConfig.class);
    }

    // Read YAML data and navigate to the root-keys path
    // Returns raw data with duplicates preserved for uniqueness validation
    static Map<String, List<String>> readYamlData(final Path yamlFile, final List<String> rootKeys) throws IOException {
        final var mapper = new ObjectMapper(new YAMLFactory());
        final Map<String, Object> root = mapper.readValue(Files.readString(yamlFile), new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        var level = (Map<String, Object>) root.get(rootKeys.get(0));
        if (level == null) {
            throw new IllegalStateException("Configuration error in " + yamlFile + ": missing root-key '" + rootKeys.get(0)
                    + "'. Check enum-generator-config.yaml root-keys path for this source.");
        }

        // Navigate through remaining root-keys
        for (var i = 1; i < rootKeys.size(); i++) {
            @SuppressWarnings("unchecked")
            final var next = (Map<String, Object>) level.get(rootKeys.get(i));
            if (next == null) {
                throw new IllegalStateException("Configuration error in " + yamlFile + ": missing root-keys path '"
                        + String.join(" → ", rootKeys.subList(0, i + 1)) + "'. Check that the YAML structure matches the root-keys in enum-generator-config.yaml.");
            }
            level = next;
        }

        return level.entrySet().stream()
            // Map each key to its extracted value list; preserve insertion order and duplicates
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> extractValuesList(entry, entry.getKey(), yamlFile.toString()),
                (v1, v2) -> v1,
                LinkedHashMap::new
            ));
    }

    @SuppressWarnings("java:S1481") // Suppress Sonar warning about unused variable; used for pattern matching type check
    static List<String> extractValuesList(final Map.Entry<String, Object> entry, final String keyName, final String sourceFile) {
        if (entry.getValue() == null) {
            return new ArrayList<>();
        }

        if (entry.getValue() instanceof final List<?> list) {
            // Validate each item is a String, preserve duplicates for validation
            return list.stream()
                .map(v -> {
                    if (!(v instanceof final String s)) {
                        throw new IllegalStateException("Data type error in " + sourceFile + " at key '" + keyName
                                + "': dictionary value must be a string or list of strings. Found " + v.getClass().getSimpleName() + " = " + v);
                    }
                    return s;
                })
                .toList();
        }

        if (entry.getValue() instanceof final String ignored) {
            return new ArrayList<>();
        }

        throw new IllegalStateException("Data structure error in " + sourceFile + " at key '" + keyName
                + "': expected a list of strings or null/empty value. Found " + entry.getValue().getClass().getSimpleName()
                + ". Check YAML syntax (should be a list with '- item' format or empty).");
    }

    // Convert list-based data to set-based for downstream use (deduplicates after validation)
    static Map<String, Set<String>> toSetMap(final Map<String, List<String>> listMap) {
        return listMap.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> new LinkedHashSet<>(entry.getValue()),
                (v1, v2) -> v1,
                LinkedHashMap::new
            ));
    }

    static String escapeForJavaStringLiteral(final String s) {
        return QUOTE.matcher(BACKSLASH.matcher(s).replaceAll("\\\\\\\\")).replaceAll("\\\\\"");
    }
}

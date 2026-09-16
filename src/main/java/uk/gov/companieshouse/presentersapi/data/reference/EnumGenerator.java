package uk.gov.companieshouse.presentersapi.data.reference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles enum generation from YAML sources.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Extract enum values from YAML data by location (keys or values)
 *   <li>Create and register new enums
 *   <li>Merge multi-source enums
 *   <li>Generate and write enum source files
 * </ul>
 */
class EnumGenerator {
    private final Map<String, GeneratedEnumDef> generatedEnums = new LinkedHashMap<>();
    private final EnumGeneratorConfig config;
    private final Path configDir;
    private final Path outputDir;
    private final GeneratorValidator validator;

    // Static patterns for enum constant normalization
    static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]+");

    EnumGenerator(final EnumGeneratorConfig config, final Path configDir, final Path outputDir) {
        this.config = config;
        this.configDir = configDir;
        this.outputDir = outputDir;
        this.validator = new GeneratorValidator(config, configDir, generatedEnums);
    }

    Map<String, GeneratedEnumDef> getGeneratedEnums() {
        return generatedEnums;
    }

    GeneratorValidator getValidator() {
        return validator;
    }

    // (Part of 6-phase pipeline in EnumBiMapGenerator.generate())
    // Phase 1: Extract enum values from YAML sources, create enums, merge multi-source enums, write enum files.
    void generateEnumsFromSources() throws IOException {
        try {
            config.sources().forEach((sourceFile, sourceConfig) -> {
                try {
                    final var yamlPath = configDir.resolve(sourceFile);
                    final var yamlDataList = EnumBiMapGenerator.readYamlData(yamlPath, sourceConfig.rootKeys());

                    // Process each enum definition in source
                    sourceConfig.enums().forEach((enumName, enumDef) -> {
                        try {
                            processEnumFromSource(enumName, enumDef, sourceFile, yamlDataList);
                        } catch (final IOException e) {
                            throw new GeneratorValidator.WrappedIOException(e);
                        }
                    });
                } catch (final IOException e) {
                    throw new GeneratorValidator.WrappedIOException(e);
                }
            });
        } catch (final GeneratorValidator.WrappedIOException e) {
            throw e.unwrap();
        }
    }

    private void processEnumFromSource(
            final String enumName, final EnumGeneratorConfig.EnumDefinition enumDef,
            final String sourceFile, final Map<String, List<String>> yamlDataList) throws IOException {
        if (generatedEnums.containsKey(enumName)) {
            mergeMultiSourceEnum(enumName, enumDef, sourceFile, yamlDataList);
        } else {
            createAndRegisterEnum(enumName, enumDef, sourceFile, yamlDataList);
        }
    }

    private void mergeMultiSourceEnum(
            final String enumName, final EnumGeneratorConfig.EnumDefinition enumDef,
            final String sourceFile, final Map<String, List<String>> yamlDataList) {
        // Validate uniqueness even for merged enums (each source independently)
        if (enumDef.validation() != null && enumDef.validation().uniquenessValidation() != null) {
            GeneratorValidator.validateUniqueness(yamlDataList, enumDef.validation().uniquenessValidation(), sourceFile);
        }

        final var yamlData = EnumBiMapGenerator.toSetMap(yamlDataList);
        final var existingValues = generatedEnums.get(enumName).values();
        final var sourceValues = extractValuesFromYaml(yamlData, enumDef.location());
        existingValues.addAll(sourceValues);
    }

    private void createAndRegisterEnum(
            final String enumName, final EnumGeneratorConfig.EnumDefinition enumDef,
            final String sourceFile, final Map<String, List<String>> yamlDataList) throws IOException {
        if (enumDef.validation() != null && enumDef.validation().uniquenessValidation() != null) {
            GeneratorValidator.validateUniqueness(yamlDataList, enumDef.validation().uniquenessValidation(), sourceFile);
        }

        final var yamlData = EnumBiMapGenerator.toSetMap(yamlDataList);
        final var enumValues = extractValuesFromYaml(yamlData, enumDef.location());
        final var generatedEnum = new GeneratedEnumDef(enumName, enumValues, config.outputPackage().enumPackage());
        generatedEnums.put(enumName, generatedEnum);
        writeEnumFile(outputDir, generatedEnum, Set.of(sourceFile));
    }

    // Extract enum values based on location (DICTIONARY_KEYS or DICTIONARY_VALUES)
    private static Set<String> extractValuesFromYaml(final Map<String, Set<String>> yamlData, final EnumGeneratorConfig.Location location) {
        if (location == EnumGeneratorConfig.Location.DICTIONARY_KEYS) {
            return new LinkedHashSet<>(yamlData.keySet());
        } else {
            // Flatten all value sets into a single set
            return yamlData.values().stream().flatMap(Set::stream).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private void writeEnumFile(final Path outputDir, final GeneratedEnumDef generatedEnum, final Set<String> sourceFiles)
            throws IOException {
        final var packagePath = outputDir.resolve(generatedEnum.outputPackage().replace('.', '/'));
        Files.createDirectories(packagePath);
        final var enumSource = buildEnumSource(generatedEnum.outputPackage(), generatedEnum.name(), generatedEnum.values(), sourceFiles);
        Files.writeString(packagePath.resolve(generatedEnum.name() + ".java"), enumSource);
    }

    static String buildEnumSource(final String pkg, final String enumName, final Set<String> codes, final Set<String> sourceFiles) {
        if (codes.isEmpty()) {
            throw new IllegalStateException("Generation error: enum '" + enumName + "' has no values extracted from ["
                    + String.join(", ", sourceFiles) + "]. Check that: (1) the location (DICTIONARY_KEYS or DICTIONARY_VALUES) is correct in enum-generator-config.yaml, "
                    + "(2) the YAML files exist and have data at the root-keys path, (3) the YAML structure is valid.");
        }

        // Build constant-to-code mapping and detect normalization collisions
        final var constantToCode = codes.stream()
            .collect(java.util.stream.Collectors.toMap(
                EnumGenerator::toEnumConstant,
                code -> code,
                (existing, code) -> {
                    if (!existing.equals(code)) {
                        throw new IllegalStateException("Generation error in enum '" + enumName + "' from [" + String.join(", ", sourceFiles)
                                + "]: normalization collision. Values '" + existing + "' and '" + code + "' both normalize to constant '" + toEnumConstant(existing)
                                + "'. Java enum constant names must be unique. Use different source values in YAML (e.g., rename one to distinguish them).");
                    }
                    return existing;
                },
                LinkedHashMap::new
            ));

        final var constantsBlock = constantToCode.entrySet().stream()
                                                  .map(e -> "    " + e.getKey() + "(\"" + EnumBiMapGenerator.escapeForJavaStringLiteral(e.getValue()) + "\")")
                                                  .collect(java.util.stream.Collectors.joining(",\n"));

        return """
            package %s;

            import com.fasterxml.jackson.annotation.JsonCreator;
            import com.fasterxml.jackson.annotation.JsonValue;

            import java.util.Arrays;
            import java.util.Map;
            import java.util.function.Function;
            import java.util.stream.Collectors;

            public enum %s {
            %s;

                private final String code;

                %s(String code) {
                    this.code = code;
                }

                @JsonValue
                public String code() {
                    return code;
                }

                private static final Map<String, %s> BY_CODE =
                    Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(%s::code, Function.identity()));

                @JsonCreator
                public static %s from(String v) {
                    final var e = BY_CODE.get(v);
                    if (e == null) throw new IllegalArgumentException("Unknown %s: " + v);
                    return e;
                }
            }
            """.formatted(
            pkg,
            enumName,
            constantsBlock,
            enumName,
            enumName,
            enumName,
            enumName,
            enumName
        );
    }

    static String toEnumConstant(final String raw) {
        var s = raw.trim().toUpperCase(java.util.Locale.ROOT);
        s = NON_ALPHANUMERIC.matcher(s).replaceAll("_");
        s = org.apache.commons.lang3.StringUtils.strip(s, "_");
        if (s.isEmpty()) {
            throw new IllegalStateException("Generation error: value '" + raw
                    + "' contains no alphanumeric characters and cannot be converted to a valid Java enum constant. Provide values that contain at least one letter (A-Z) or number (0-9).");
        }
        if (Character.isDigit(s.charAt(0))) {
            s = "_" + s;
        }
        return s;
    }
}

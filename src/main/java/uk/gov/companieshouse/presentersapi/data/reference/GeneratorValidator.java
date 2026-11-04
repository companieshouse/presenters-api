package uk.gov.companieshouse.presentersapi.data.reference;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

/**
 * Handles all validation logic for enum and bimap generation.
 *
 * <p>In the seven-phase pipeline:
 *   <ul>
 *       <li>{@link EnumGenerator#generateEnumsFromSources()} performs
 * Phase 1 and calls {@link #validateUniqueness(Map, EnumGeneratorConfig.UniquenessValidation, String)}.
 *       </li>
 *     <li>This class performs validation Phases 2-6;</li>
 *     <li>{@link BiMapGenerator#generateBiMapsFromSources()} performs Phase 7.</li>
 *   </ul>
 * </p>
 *
 * <p>Responsibilities:
 *   <ul>
 *     <li>Uniqueness validation (VALUES_PER_KEY, VALUES_PER_KEY_SET)</li>
 *     <li>Cross-source reference validation</li>
 *     <li>Key-value conflict detection within and across sources</li>
 *     <li>Value ownership validation (no foreign enum tokens)</li>
 *   </ul>
 * </p>
 */
class GeneratorValidator {
    private static final String APP_NAMESPACE = "presenters-api";
    private static final Logger logger = LoggerFactory.getLogger(APP_NAMESPACE);
    public static final String DATA_VALIDATION_ERROR_IN = "Data validation error in ";

    private final EnumGeneratorConfig config;
    private final Path configDir;
    private final Map<String, GeneratedEnumDef> generatedEnums;

    GeneratorValidator(final EnumGeneratorConfig config, final Path configDir, final Map<String, GeneratedEnumDef> generatedEnums) {
        this.config = config;
        this.configDir = configDir;
        this.generatedEnums = generatedEnums;
    }

    // (Part of 7-phase pipeline in EnumBiMapGenerator.generate())
    // Phase 2: Validate that dictionary keys and values don't overlap within each source file.
    void validateNoKeyValueConflictsWithinSources() throws IOException {
        try {
            config.sources().forEach((sourceFile, sourceConfig) -> {
                try {
                    final var yamlPath = configDir.resolve(sourceFile);
                    final var yamlDataList = GeneratorData.readYamlData(yamlPath, sourceConfig.rootKeys());
                    final var yamlData = GeneratorData.toSetMap(yamlDataList);

                    final var allKeysInSource = yamlData.keySet();

                    // Find first conflicting value across all values in value-location enums
                    sourceConfig.enums().values().stream()
                        .filter(enumDef -> enumDef.location() == EnumGeneratorConfig.Location.DICTIONARY_VALUES)
                        .flatMap(enumDef -> yamlData.values().stream()
                            .flatMap(Set::stream))
                        .filter(allKeysInSource::contains)
                        .findFirst()
                        .ifPresent(conflictingValue -> reportConflict(sourceFile, conflictingValue));
                } catch (final IOException e) {
                    throw new WrappedIOException(e);
                }
            });
        } catch (final WrappedIOException e) {
            throw e.unwrap();
        }
    }

    private void reportConflict(final String sourceFile, final String conflictingValue) {
        throw new IllegalStateException(
            DATA_VALIDATION_ERROR_IN + sourceFile + ": conflicting data.\n"
            + "  '" + conflictingValue + "' is used as both a mapping key and a mapping value.\n"
            + "  Mapping keys and values must be different — each value should have one role.\n"
            + "  To fix: Remove '" + conflictingValue + "' from either the keys or values, or rename it to resolve the conflict."
        );
    }

    // (Part of 7-phase pipeline in EnumBiMapGenerator.generate())
    // Phase 3: Validate that enum values marked for validation in other sources are actually present as keys there.
    void validateReferencedKeysInOtherSources() throws IOException {
        // Map enum names to their definition source files, grouping by enum name
        final var enumToSourceFiles = config.sources().entrySet().stream()
            .flatMap(sourceEntry -> sourceEntry.getValue().enums().entrySet().stream()
                .filter(enumEntry -> shouldValidateAsKeysInOtherSources(enumEntry.getValue()))
                .map(enumEntry -> Map.entry(enumEntry.getKey(), sourceEntry.getKey())))
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toCollection(LinkedHashSet::new))
            ));

        // Auto-discover and validate each enum in other sources
        try {
            enumToSourceFiles.forEach((enumName, sourceFiles) -> {
                try {
                    final var enumValues = generatedEnums.get(enumName).values();
                    final var referencedInFiles = scanForEnumKeysInOtherSources(enumName, enumValues, sourceFiles);
                    if (!referencedInFiles.isEmpty()) {
                        logger.info("  [Auto-discovery] Enum '" + enumName + "' used as dictionary keys in: " + referencedInFiles);
                    }
                } catch (final IOException e) {
                    // Wrap a checked IOException to propagate through forEach unwrapped below
                    throw new WrappedIOException(e);
                }
            });
        } catch (final WrappedIOException e) {
            // Unwrap and rethrow checked IOException with original exception semantics
            throw e.unwrap();
        }
    }

    private boolean shouldValidateAsKeysInOtherSources(final EnumGeneratorConfig.EnumDefinition enumDef) {
        return enumDef.validation() != null
            && enumDef.validation().validateAsKeysInOtherSources() != null
            && enumDef.validation().validateAsKeysInOtherSources();
    }

    // Auto-scan all sources looking for a specific enum used as dictionary keys
    private Set<String> scanForEnumKeysInOtherSources(
            final String targetEnumName, final Set<String> validEnumValues, final Set<String> definitionSources)
            throws IOException {
        try {
            // Filter non-matching sources, then validate each for this enum as dictionary keys
            return config.sources().entrySet().stream()
                .filter(sourceEntry -> !definitionSources.contains(sourceEntry.getKey()))
                .filter(sourceEntry -> sourceEntry.getValue().enums().values().stream()
                    .anyMatch(enumDef -> targetEnumName.equals(enumDef.dictionaryKeysReference())))
                .map(sourceEntry -> {
                    try {
                        final var yamlPath = configDir.resolve(sourceEntry.getKey());
                        final var yamlDataList = GeneratorData.readYamlData(yamlPath, sourceEntry.getValue().rootKeys());
                        final var yamlData = GeneratorData.toSetMap(yamlDataList);
                        validateDictionaryKeysMatch(yamlData, validEnumValues, targetEnumName, yamlPath, true, definitionSources);
                        return sourceEntry.getKey();
                    } catch (final IOException e) {
                        // Wrap checked IOException to propagate through stream; unwrapped below
                        throw new WrappedIOException(e);
                    }
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (final WrappedIOException e) {
            // Unwrap and rethrow checked IOException with original exception semantics
            throw e.unwrap();
        }
    }

    // (Part of 7-phase pipeline in EnumBiMapGenerator.generate())
    // Phase 4: Validate that dictionary keys and values don't conflict across different enum roles.
    void validateNoValueConflictsAcrossRoles() throws IOException {
        // First pass: collect all keys by enum
        final var valueToKeyEnum = collectValuesByLocation(EnumGeneratorConfig.Location.DICTIONARY_KEYS);

        // Second pass: collect all values and check for conflicts with DIFFERENT enums
        collectValuesByLocation(EnumGeneratorConfig.Location.DICTIONARY_VALUES)
            .forEach((value, valueEnum) -> {
                final var keyEnumForValue = valueToKeyEnum.get(value);
                if (keyEnumForValue != null && !keyEnumForValue.equals(valueEnum)) {
                    throw new IllegalStateException(
                        "Conflicting value usage: '" + value + "' cannot be used as both a mapping key and mapping value.\n"
                        + "  It's used as a key for enum: " + keyEnumForValue + "\n"
                        + "  And as a value for enum: " + valueEnum + "\n"
                        + "  To fix: Ensure '" + value + "' appears in only one role across all files."
                    );
                }
            });
    }

    private Map<String, String> collectValuesByLocation(final EnumGeneratorConfig.Location location)
            throws IOException {
        final var valueToEnum = new LinkedHashMap<String, String>();
        try {
            // Flatten all sources, then all enums, filtering for location; extract and map values to enum names
            config.sources().entrySet().stream()
                .flatMap(sourceEntry -> {
                    try {
                        final var yamlPath = configDir.resolve(sourceEntry.getKey());
                        final var yamlDataList = GeneratorData.readYamlData(yamlPath, sourceEntry.getValue().rootKeys());
                        final var yamlData = GeneratorData.toSetMap(yamlDataList);
                        // Filter enums by location, flatten to individual values with their enum names
                        return sourceEntry.getValue().enums().entrySet().stream()
                            .filter(enumEntry -> location == enumEntry.getValue().location())
                            .flatMap(enumEntry -> {
                                final var enumName = enumEntry.getKey();
                                final var values = location == EnumGeneratorConfig.Location.DICTIONARY_KEYS
                                    ? yamlData.keySet()
                                    : yamlData.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
                                return values.stream()
                                    .map(value -> new AbstractMap.SimpleEntry<>(value, enumName));
                            });
                    } catch (final IOException e) {
                        // Wrap checked IOException to propagate through stream; unwrapped below
                        throw new WrappedIOException(e);
                    }
                })
                .forEach(entry -> valueToEnum.put(entry.getKey(), entry.getValue()));
        } catch (final WrappedIOException e) {
            // Unwrap and rethrow checked IOException with original exception semantics
            throw e.unwrap();
        }
        return valueToEnum;
    }

    private static void validateDictionaryKeysMatch(
            final Map<String, Set<String>> yamlData, final Set<String> validEnumValues,
            final String enumName, final Path sourceFile, final boolean isKeyValidation,
            final Set<String> definitionSources) {
        final var invalidItems = yamlData.keySet().stream()
                                        .filter(key -> !validEnumValues.contains(key))
                                        .collect(Collectors.toSet());
        if (!invalidItems.isEmpty()) {
            final var itemType = isKeyValidation ? "key" : "value";
            final var hint = isKeyValidation 
                ? "\n  Hint: Check if these are defined as values (not keys) in other files."
                : "";
            
            throw new IllegalStateException(
                DATA_VALIDATION_ERROR_IN + sourceFile + ": invalid " + itemType + "(s) — not valid " + enumName + ".\n"
                + "  Invalid: " + invalidItems + "\n"
                + "  Valid " + enumName + " values: " + validEnumValues + hint + "\n"
                + "  To fix: Replace with valid values from the list above, or, if the key is correct, check whether "
                + "it is missing from the source data defining " + enumName + ": " + definitionSources + ".\n"
                + "  Add the missing code at the configured extraction location (DICTIONARY_KEYS or DICTIONARY_VALUES) "
                + "in the original source YAML and regenerate. Adding a key to a mapping that only references "
                + enumName + " does not define a new enum value."
            );
        }
    }

    /**
     * Phase 1: Validates uniqueness constraints during enum extraction by {@link EnumGenerator}.
     *
     * <p>This method enforces uniqueness validation constraints configured in enum-generator-config.yaml:
     *
     * <ul>
     *   <li><b>VALUES_PER_KEY_SET:</b> Validates that each dictionary value appears under AT MOST ONE key.
     *       Example: If FormType "AD01" appears in both "firm-delivery" and "individual-delivery",
     *       throws IllegalStateException because the constraint is violated.
     *       This constraint becomes the reverse BiMap type: singular EnumMap&lt;FormType, FormGroup&gt;.
     *   <li><b>VALUES_PER_KEY:</b> Validates that each dictionary value does NOT appear multiple times
     *       under the SAME key. But values are allowed to repeat across different keys.
     *       Example: If PresenterType "officer-employee" appears under both "firm-delivery" and
     *       "individual-delivery" keys, that's OK. But if it appeared twice under "firm-delivery",
     *       that would violate the constraint.
     *       This constraint becomes the reverse BiMap type: plural EnumMap&lt;PresenterType, EnumSet&lt;FormGroup&gt;&gt;.
     * </ul>
     *
     * @param dataMap the dictionary data where keys map to lists of values (preserves duplicates)
     * @param uniquenessValidation the uniqueness validation constraint to enforce
     * @param sourceFile the source file name (for error messages)
     * @throws IllegalStateException if the uniqueness validation constraint is violated
     */
    static void validateUniqueness(final Map<String, List<String>> dataMap, final EnumGeneratorConfig.UniquenessValidation uniquenessValidation, final String sourceFile) {
        if (uniquenessValidation == EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET) {
            // Flatten all key-value pairs into (value, key) entries, then group by value to see
            // how many different keys each value appears under. If any value has >1 key, constraint violated.
            dataMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                    .map(value -> new AbstractMap.SimpleEntry<>(value, entry.getKey())))
                .collect(Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toCollection(LinkedHashSet::new))
                ))
                .forEach((value, keys) -> {
                    if (keys.size() > 1) {
                        reportUniquenessViolationAcrossKeys(sourceFile, value, keys);
                    }
                });
        } else if (uniquenessValidation == EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY) {
            // For each key, check if any value appears more than once in its value list.
            dataMap.forEach((key, values) -> {
                final var duplicates = values.stream()
                    .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .findFirst();

                duplicates.ifPresent(stringLongEntry -> reportUniquenessViolationWithinKey(sourceFile,
                    stringLongEntry.getKey(),
                    key));
            });
        }
    }

    private static void reportUniquenessViolationAcrossKeys(final String sourceFile, final String value, final Set<String> keys) {
        throw new IllegalStateException(
            "Validation error in " + sourceFile + ": uniqueness constraint VALUES_PER_KEY_SET failed.\n"
            + "  Dictionary value '" + value + "' appears under multiple dictionary keys: " + keys + "\n"
            + "  Constraint: Each DICTIONARY_VALUE must belong to exactly one DICTIONARY_KEY.\n"
            + "  To fix: Ensure this value appears only once, or split it into distinct values."
        );
    }

    private static void reportUniquenessViolationWithinKey(final String sourceFile, final String value, final String key) {
        throw new IllegalStateException(
            "Validation error in " + sourceFile + ": uniqueness constraint VALUES_PER_KEY failed.\n"
            + "  Dictionary value '" + value + "' appears multiple times under key '" + key + "'.\n"
            + "  Constraint: Each DICTIONARY_VALUE under a key must be unique (no duplicates per key).\n"
            + "  To fix: Remove duplicate entries or consolidate them."
        );
    }

    // (Part of 7-phase pipeline in EnumBiMapGenerator.generate())
    // Phase 5: Validate that enum values appear only in sources where they are defined with correct roles (no foreign enums).
    void validateNoForeignEnumValuesInSources() throws IOException {
        // Build global value -> owning enum index and enforce single ownership
        final var valueToOwnerEnum = buildValueToOwnerEnumIndex();

        try {
            // For each source, validate that keys/values belong to allowed enums by role
            config.sources().forEach((sourceFile, sourceConfig) -> {
                try {
                    final var yamlPath = configDir.resolve(sourceFile);
                    final var yamlDataList = GeneratorData.readYamlData(yamlPath, sourceConfig.rootKeys());
                    final var yamlData = GeneratorData.toSetMap(yamlDataList);

                    // Use source-level key-enum and value-enum fields to define allowed enum types per role
                    final Set<String> allowedKeyEnums = sourceConfig.keyEnum() != null
                        ? Set.of(sourceConfig.keyEnum())
                        : Set.of();
                    final Set<String> allowedValueEnums = sourceConfig.valueEnum() != null
                        ? Set.of(sourceConfig.valueEnum())
                        : Set.of();

                    // Validate dictionary keys and values using streams
                    yamlData.keySet().forEach(key ->
                        validateTokenOwnership(sourceFile, "DICTIONARY_KEY", key, valueToOwnerEnum, allowedKeyEnums)
                    );

                    yamlData.values().stream()
                        .flatMap(Set::stream)
                        .forEach(value ->
                            validateTokenOwnership(sourceFile, "DICTIONARY_VALUE", value, valueToOwnerEnum, allowedValueEnums)
                        );
                } catch (final IOException e) {
                    throw new WrappedIOException(e);
                }
            });
        } catch (final WrappedIOException e) {
            throw e.unwrap();
        }
    }

    // (Part of 7-phase pipeline in EnumBiMapGenerator.generate())
    // Phase 6: Require non-empty target mappings for non-excluded source keys with values.
    void validateNonEmptyValuesInTargets() throws IOException {
        for (final var source : config.sources().entrySet()) {
            validateSourceCoverage(source.getKey(), source.getValue());
        }
    }

    private void validateSourceCoverage(
            final String sourceFile, final EnumGeneratorConfig.Source sourceConfig) throws IOException {
        if (sourceConfig.requireNonEmptyValuesIn().isEmpty()) {
            return;
        }

        final var sourceData = GeneratorData.readYamlData(
            configDir.resolve(sourceFile), sourceConfig.rootKeys());
        for (final var requirement : sourceConfig.requireNonEmptyValuesIn()) {
            validateCoverageRequirement(sourceFile, sourceData, requirement);
        }
    }

    private void validateCoverageRequirement(
            final String sourceFile, final Map<String, List<String>> sourceData,
            final EnumGeneratorConfig.NonEmptyValuesRequirement requirement) throws IOException {
        final var targetConfig = validateCoverageTarget(config, sourceFile, requirement);
        final var targetData = GeneratorData.readYamlData(
            configDir.resolve(requirement.target()), targetConfig.rootKeys());

        warnAboutStaleCoverageExclusions(sourceFile, sourceData, requirement);
        final var uncovered = findUncoveredKeys(sourceData, targetData, requirement.excludedKeys());
        if (!uncovered.isEmpty()) {
            reportUncoveredKeys(sourceFile, requirement.target(), uncovered);
        }
    }

    private void warnAboutStaleCoverageExclusions(
            final String sourceFile, final Map<String, List<String>> sourceData,
            final EnumGeneratorConfig.NonEmptyValuesRequirement requirement) {
        for (final var staleKey : staleExcludedKeys(sourceData, requirement.excludedKeys())) {
            logger.info("  [Warning] Coverage exclusion '" + staleKey + "' in " + sourceFile
                + " for target " + requirement.target() + " is absent or has no mapped values. "
                + "The excluded-keys in enum-generator-config.yaml may need updating if '" + staleKey
                + "' is no longer needed.");
        }
    }

    private static Map<String, List<String>> findUncoveredKeys(
            final Map<String, List<String>> sourceData, final Map<String, List<String>> targetData,
            final List<String> excludedKeys) {
        final var uncovered = new LinkedHashMap<String, List<String>>();
        sourceData.forEach((key, values) -> {
            if (!values.isEmpty() && !excludedKeys.contains(key)
                    && (!targetData.containsKey(key) || targetData.get(key).isEmpty())) {
                uncovered.put(key, values);
            }
        });
        return uncovered;
    }

    private static void reportUncoveredKeys(
            final String sourceFile, final String targetFile, final Map<String, List<String>> uncovered) {
        throw new IllegalStateException(DATA_VALIDATION_ERROR_IN + sourceFile
            + ": missing non-empty mappings in target " + targetFile + ".\n"
            + "  Uncovered keys and affected source values: " + uncovered + "\n"
            + "  To fix: Add non-empty target mappings for these keys, or explicitly exclude "
            + "business-approved keys using excluded-keys in enum-generator-config.yaml.");
    }

    // Phase 6 helper, also used before resource generation to validate targets before path remapping.
    static EnumGeneratorConfig.Source validateCoverageTarget(
            final EnumGeneratorConfig config, final String sourceFile,
            final EnumGeneratorConfig.NonEmptyValuesRequirement requirement) {
        final var target = requirement.target();
        if (target == null || target.isBlank() || !config.sources().containsKey(target)) {
            throw new IllegalStateException("Configuration error in " + sourceFile
                + ": require-non-empty-values-in target '" + target
                + "' must identify an entry in sources in enum-generator-config.yaml.");
        }
        final var sourceConfig = config.sources().get(sourceFile);
        final var targetConfig = config.sources().get(target);
        if (sourceConfig.keyEnum() == null || sourceConfig.keyEnum().isBlank()
                || !sourceConfig.keyEnum().equals(targetConfig.keyEnum())) {
            throw new IllegalStateException("Configuration error in " + sourceFile
                + ": coverage target " + target + " must declare the same non-blank key-enum. "
                + "Source key-enum: " + sourceConfig.keyEnum() + "; target key-enum: " + targetConfig.keyEnum());
        }
        return targetConfig;
    }

    // Phase 6 helper: Identify exclusions that need a reminder because their source keys are absent or empty.
    static Set<String> staleExcludedKeys(
            final Map<String, List<String>> sourceData, final List<String> excludedKeys) {
        return excludedKeys.stream()
            .filter(key -> !sourceData.containsKey(key) || sourceData.get(key).isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Build a global index mapping each enum value to its owning enum type.
    // Enforces the rule that each value belongs to exactly one enum (single ownership).
    private Map<String, String> buildValueToOwnerEnumIndex() {
        final var valueToOwnerEnum = new LinkedHashMap<String, String>();
        // Flatten all enum values and track ownership, enforcing single ownership per value
        generatedEnums.forEach((enumName, enumDef) ->
            enumDef.values().forEach(value -> {
                final var existingOwner = valueToOwnerEnum.put(value, enumName);
                if (existingOwner != null) {
                    reportDuplicateValueOwner(value, existingOwner, enumName);
                }
            })
        );
        return valueToOwnerEnum;
    }

    // Validate that a token (key or value) found in YAML belongs to one of the allowed enum types for its role.
    // Resolves the token's ownership from the global value index and checks membership in allowed set.
    private void validateTokenOwnership(
            final String sourceFile, final String role, final String token,
            final Map<String, String> valueToOwnerEnum, final Set<String> allowedEnums) {
        final var ownerEnum = valueToOwnerEnum.get(token);
        if (ownerEnum == null) {
            throw new IllegalStateException(
                DATA_VALIDATION_ERROR_IN + sourceFile + ": unknown enum token in " + role + ".\n"
                + "  Token '" + token + "' does not belong to any known enum type.\n"
                + "  This role ('" + role + "') expects tokens from enums: " + allowedEnums + ".\n"
                + "  To fix: Verify '" + token + "' is defined in the correct source YAML file."
            );
        }
        if (!allowedEnums.contains(ownerEnum)) {
            reportForeignEnumToken(sourceFile, role, token, ownerEnum, allowedEnums);
        }
    }

    // Report an error when a token belongs to a disallowed enum for its source role.
    // Error includes source, role, token, detected owner, and expected allowed enums to guide remediation.
    private void reportForeignEnumToken(
            final String sourceFile, final String role, final String token,
            final String detectedOwnerEnum, final Set<String> allowedEnums) {
        throw new IllegalStateException(
            DATA_VALIDATION_ERROR_IN + sourceFile + ": foreign enum token in " + role + ".\n"
            + "  Token '" + token + "' belongs to enum '" + detectedOwnerEnum + "'.\n"
            + "  This role ('" + role + "') only allows enums: " + allowedEnums + ".\n"
            + "  To fix: Verify '" + token + "' is the correct value; or move it to a source where "
            + detectedOwnerEnum + " is an allowed " + role + "."
        );
    }

    // Report an error when the same value is owned by multiple distinct enum types.
    // This violates the global single-ownership rule and must be fixed in the source enums.
    private void reportDuplicateValueOwner(final String value, final String firstOwner, final String secondOwner) {
        throw new IllegalStateException(
            "Generation error: enum value '" + value + "' is defined in multiple enums.\n"
            + "  First owner: '" + firstOwner + "'.\n"
            + "  Second owner: '" + secondOwner + "'.\n"
            + "  Constraint: Each enum value must belong to exactly one enum type.\n"
            + "  To fix: Remove '" + value + "' from one of the source YAML files, or rename it to distinguish them."
        );
    }

    // Helper to wrap checked IOException for use in forEach and stream operations.
    // Stream lambdas cannot throw checked exceptions, so we wrap IOException in this unchecked exception,
    // then unwrap and rethrow it in the surrounding try-catch. This allows checked exceptions to propagate
    // through stream pipelines while maintaining proper exception type semantics.
    static class WrappedIOException extends RuntimeException {
        WrappedIOException(final IOException cause) {
            super(cause);
        }
        IOException unwrap() {
            return (IOException) getCause();
        }
    }
}

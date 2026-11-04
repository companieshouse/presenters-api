package uk.gov.companieshouse.presentersapi.data.reference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * Wraps documentation dictionaries in the root keys used by generated BiMap bindings.
 */
class YamlResourceGenerator {

    private YamlResourceGenerator() {
    }

    static EnumGeneratorConfig generate(
            final EnumGeneratorConfig config, final Path configDir, final Path resourcesDir) throws IOException {
        final var mapper = createMapper();
        validateCoverageTargets(config);
        final var generatedSources = generateSourceResources(config, configDir, resourcesDir, mapper);
        return remapConfig(config, configDir, generatedSources);
    }

    private static ObjectMapper createMapper() {
        return new ObjectMapper(YAMLFactory.builder()
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .build());
    }

    private static void validateCoverageTargets(final EnumGeneratorConfig config) {
        for (final var source : config.sources().entrySet()) {
            for (final var requirement : source.getValue().requireNonEmptyValuesIn()) {
                GeneratorValidator.validateCoverageTarget(config, source.getKey(), requirement);
            }
        }
    }

    private static Map<String, EnumGeneratorConfig.Source> generateSourceResources(
            final EnumGeneratorConfig config, final Path configDir, final Path resourcesDir,
            final ObjectMapper mapper) throws IOException {
        final var generatedSources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        for (final var source : config.sources().entrySet()) {
            writeSourceResource(source.getKey(), source.getValue(), configDir, resourcesDir, mapper, generatedSources);
        }
        return generatedSources;
    }

    private static void writeSourceResource(
            final String sourceFile, final EnumGeneratorConfig.Source source, final Path configDir,
            final Path resourcesDir, final ObjectMapper mapper,
            final Map<String, EnumGeneratorConfig.Source> generatedSources) throws IOException {
        final var sourcePath = configDir.resolve(sourceFile).normalize();
        final var rootKeys = source.rootKeys();
        validateRootKeys(sourcePath, rootKeys);

        final var resourceName = "data/" + sourcePath.getFileName();
        if (generatedSources.putIfAbsent(resourceName, source) != null) {
            throw new IllegalStateException("Duplicate generated resource filename: " + resourceName);
        }

        final var dictionary = readDictionary(sourcePath, mapper);
        final var normalised = normaliseDictionary(dictionary, sourcePath);
        final var wrapped = Map.of(rootKeys.getFirst(), Map.of(rootKeys.getLast(), normalised));
        final var output = resourcesDir.resolve(resourceName);
        Files.createDirectories(output.getParent());
        Files.writeString(output, mapper.writeValueAsString(wrapped));
    }

    private static void validateRootKeys(final Path sourcePath, final List<String> rootKeys) {
        if (rootKeys == null || rootKeys.size() != 2
                || rootKeys.stream().anyMatch(key -> key == null || key.isBlank())) {
            throw new IllegalStateException("Expected two non-blank binding root-keys for " + sourcePath);
        }
    }

    private static Map<String, Object> readDictionary(final Path sourcePath, final ObjectMapper mapper)
            throws IOException {
        final Map<String, Object> dictionary = mapper.readValue(
            Files.readString(sourcePath), new TypeReference<>() {});
        if (dictionary == null || dictionary.isEmpty()) {
            throw new IllegalStateException("Expected a non-empty YAML dictionary in " + sourcePath);
        }
        return dictionary;
    }

    private static Map<String, List<String>> normaliseDictionary(
            final Map<String, Object> dictionary, final Path sourcePath) {
        final var normalised = new LinkedHashMap<String, List<String>>();
        for (final var entry : dictionary.entrySet()) {
            // Documentation permits scalars; runtime bindings and validation use lists.
            normalised.put(entry.getKey(), entry.getValue() instanceof final String value
                ? List.of(value)
                : GeneratorData.extractValuesList(entry, entry.getKey(), sourcePath.toString()));
        }
        return normalised;
    }

    private static EnumGeneratorConfig remapConfig(
            final EnumGeneratorConfig config, final Path configDir,
            final Map<String, EnumGeneratorConfig.Source> generatedSources) {
        final var remappedSources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        generatedSources.forEach((resourceName, source) -> {
            final var requirements = source.requireNonEmptyValuesIn().stream()
                .map(requirement -> remapRequirement(configDir, requirement))
                .toList();
            remappedSources.put(resourceName, new EnumGeneratorConfig.Source(
                source.rootKeys(), source.keyEnum(), source.valueEnum(), source.enums(), requirements));
        });
        return new EnumGeneratorConfig(config.outputPackage(), remappedSources);
    }

    private static EnumGeneratorConfig.NonEmptyValuesRequirement remapRequirement(
            final Path configDir, final EnumGeneratorConfig.NonEmptyValuesRequirement requirement) {
        return new EnumGeneratorConfig.NonEmptyValuesRequirement(
            "data/" + configDir.resolve(requirement.target()).normalize().getFileName(),
            requirement.excludedKeys());
    }
}

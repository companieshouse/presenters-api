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
        final var mapper = new ObjectMapper(YAMLFactory.builder()
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .build());
        final var generatedSources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        for (final var source : config.sources().entrySet()) {
            final var sourcePath = configDir.resolve(source.getKey()).normalize();
            final var rootKeys = source.getValue().rootKeys();
            if (rootKeys == null || rootKeys.size() != 2
                    || rootKeys.stream().anyMatch(key -> key == null || key.isBlank())) {
                throw new IllegalStateException("Expected two non-blank binding root-keys for " + sourcePath);
            }
            final var resourceName = "data/" + sourcePath.getFileName();
            if (generatedSources.putIfAbsent(resourceName, source.getValue()) != null) {
                throw new IllegalStateException("Duplicate generated resource filename: " + resourceName);
            }

            final Map<String, Object> dictionary = mapper.readValue(
                Files.readString(sourcePath), new TypeReference<>() {});
            if (dictionary == null || dictionary.isEmpty()) {
                throw new IllegalStateException("Expected a non-empty YAML dictionary in " + sourcePath);
            }
            final var normalised = new LinkedHashMap<String, List<String>>();
            for (final var entry : dictionary.entrySet()) {
                // Documentation permits scalars; runtime bindings and validation use lists.
                normalised.put(entry.getKey(), entry.getValue() instanceof final String value
                    ? List.of(value)
                    : EnumBiMapGenerator.extractValuesList(entry, entry.getKey(), sourcePath.toString()));
            }
            final var wrapped = Map.of(rootKeys.getFirst(), Map.of(rootKeys.getLast(), normalised));
            final var output = resourcesDir.resolve(resourceName);
            Files.createDirectories(output.getParent());
            Files.writeString(output, mapper.writeValueAsString(wrapped));
        }
        return new EnumGeneratorConfig(config.outputPackage(), generatedSources);
    }
}

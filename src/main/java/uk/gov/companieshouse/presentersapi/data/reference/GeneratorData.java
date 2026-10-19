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
 * Shared data loading and formatting for the reference-data generation pipeline.
 */
final class GeneratorData {
    private static final Pattern BACKSLASH = Pattern.compile("\\\\");
    private static final Pattern QUOTE = Pattern.compile("\"");

    private GeneratorData() {
    }

    static EnumGeneratorConfig readConfig(final Path configFile) throws IOException {
        final var mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(Files.readString(configFile), EnumGeneratorConfig.class);
    }

    // Preserve duplicates for uniqueness validation.
    static Map<String, List<String>> readYamlData(final Path yamlFile, final List<String> rootKeys) throws IOException {
        final var mapper = new ObjectMapper(new YAMLFactory());
        final Map<String, Object> root = mapper.readValue(Files.readString(yamlFile), new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        var level = (Map<String, Object>) root.get(rootKeys.getFirst());
        if (level == null) {
            throw new IllegalStateException("Configuration error in " +
                                            yamlFile +
                                            ": missing root-key '" +
                                            rootKeys.getFirst() +
                                            "'. Check enum-generator-config.yaml root-keys path for this source.");
        }

        for (var i = 1; i < rootKeys.size(); i++) {
            @SuppressWarnings("unchecked")
            final var next = (Map<String, Object>) level.get(rootKeys.get(i));
            if (next == null) {
                throw new IllegalStateException("Configuration error in " +
                                                yamlFile +
                                                ": missing root-keys path '" +
                                                String.join(" → ", rootKeys.subList(0, i + 1)) +
                                                "'. Check that the YAML structure matches the root-keys in enum-generator-config.yaml.");
            }
            level = next;
        }

        final var result = new LinkedHashMap<String, List<String>>();
        level.entrySet().forEach(entry ->
            result.put(entry.getKey(), extractValuesList(entry, entry.getKey(), yamlFile.toString())));
        return result;
    }

    @SuppressWarnings("java:S1481")
    static List<String> extractValuesList(final Map.Entry<String, Object> entry, final String keyName, final String sourceFile) {
        if (entry.getValue() == null) {
            return new ArrayList<>();
        }

        if (entry.getValue() instanceof final List<?> list) {
            return list.stream()
                .map(v -> {
                    if (!(v instanceof final String s)) {
                        throw new IllegalStateException("Data type error in " +
                                                        sourceFile +
                                                        " at key '" +
                                                        keyName +
                                                        "': dictionary value must be a string or list of strings. Found " +
                                                        v.getClass().getSimpleName() +
                                                        " = " +
                                                        v);
                    }
                    return s;
                })
                .toList();
        }

        // Wrapped runtime YAML must contain lists, not scalar shorthand.
        throw new IllegalStateException("Data structure error in " +
                                        sourceFile +
                                        " at key '" +
                                        keyName +
                                        "': expected a list of strings or null/empty value. Found " +
                                        entry.getValue().getClass().getSimpleName() +
                                        ". Check YAML syntax (should be a list with '- item' format or empty).");
    }

    // Only discard duplicates after uniqueness validation.
    static Map<String, Set<String>> toSetMap(final Map<String, List<String>> listMap) {
        final var result = new LinkedHashMap<String, Set<String>>();
        listMap.forEach((key, values) -> result.put(key, new LinkedHashSet<>(values)));
        return result;
    }

    static String escapeForJavaStringLiteral(final String s) {
        return QUOTE.matcher(BACKSLASH.matcher(s).replaceAll("\\\\\\\\")).replaceAll("\\\\\"");
    }
}

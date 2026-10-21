package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.FormGroup;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.FormTypeBindingConverter;

class YamlResourceGeneratorTest {

    @TempDir
    Path tempDir;

    private static EnumGeneratorConfig config(final Map<String, EnumGeneratorConfig.Source> sources) {
        return new EnumGeneratorConfig(new EnumGeneratorConfig.OutputPackage("uk.test.enums", "uk.test.bimaps"), sources);
    }

    private static EnumGeneratorConfig.Source source(final List<String> rootKeys) {
        return new EnumGeneratorConfig.Source(rootKeys, "Group", "Item", Map.of());
    }

    private Path resources() {
        return tempDir.resolve("generated-resources");
    }

    @Test
    void shouldBindEveryFormCodeUsingGeneratedConverters() throws IOException {
        final var configFile = Path.of("src/main/resources/enum-generator-config.yaml");
        YamlResourceGenerator.generate(GeneratorData.readConfig(configFile), configFile.getParent(), resources());
        final var resource = resources().resolve("data/form-group-to-form.yaml");
        final var propertySources = new YamlPropertySourceLoader().load("forms", new FileSystemResource(resource));
        final var dictionary = GeneratorData.readYamlData(resource, List.of("form-types-map", "by-form-group"));

        new ApplicationContextRunner().withUserConfiguration(FormBindingConfiguration.class)
            .withInitializer(context -> propertySources.forEach(
                source -> context.getEnvironment().getPropertySources().addLast(source)))
            .run(context -> {
                assertThat("Application context should start successfully", context.getStartupFailure(), is(nullValue()));
                final var bound = context.getBean(FormGroupFormTypeBiMap.class);
                for (final var entry : dictionary.entrySet()) {
                    final var group = FormGroup.from(entry.getKey());
                    final var expected = entry.getValue().stream().map(FormType::from)
                        .collect(java.util.stream.Collectors.toSet());
                    assertThat(bound.formTypesFor(group), is(expected));
                    for (final var form : expected) {
                        assertThat(bound.formGroupFor(form), is(group));
                    }
                }
            });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackageClasses = FormTypeBindingConverter.class)
    @EnableConfigurationProperties(FormGroupFormTypeBiMap.class)
    static class FormBindingConfiguration {
    }

    @Test
    void shouldMergePresenterTypesFromValuesAndAllFourMappingKeySources() throws IOException {
        final var configFile = Path.of("src/main/resources/enum-generator-config.yaml");
        final var original = GeneratorData.readConfig(configFile);
        final var generated = YamlResourceGenerator.generate(original, configFile.getParent(), resources());
        final var expected = new LinkedHashSet<String>();
        final var definitions = new LinkedHashSet<String>();
        for (final var entry : generated.sources().entrySet()) {
            final var definition = entry.getValue().enums().get("PresenterType");
            if (definition == null) {
                continue;
            }
            definitions.add(entry.getKey());
            final var dictionary = GeneratorData.readYamlData(
                resources().resolve(entry.getKey()), entry.getValue().rootKeys());
            if (entry.getKey().equals("data/form-group-to-presenter-types.yaml")) {
                assertThat(definition.location(), is(EnumGeneratorConfig.Location.DICTIONARY_VALUES));
                dictionary.values().forEach(expected::addAll);
            } else {
                assertThat(definition.location(), is(EnumGeneratorConfig.Location.DICTIONARY_KEYS));
                expected.addAll(dictionary.keySet());
            }
        }
        assertThat(definitions, is(java.util.Set.of(
            "data/form-group-to-presenter-types.yaml", "data/presenter-type-to-delivery-statement.yaml",
            "data/presenter-type-to-verification-statement.yaml", "data/presenter-type-to-assoc-statement.yaml",
            "data/presenter-type-to-data-obj.yaml")));
        final var generator = new EnumGenerator(generated, resources(), tempDir.resolve("java"));

        generator.generateEnumsFromSources();

        assertThat(generator.getGeneratedEnums().get("PresenterType").values(), is(expected));
        assertThat(expected.contains("official-receiver"), is(true));
        generator.getValidator().validateReferencedKeysInOtherSources();
    }

    @Test
    void shouldWrapAndNormaliseDictionaryWithoutChangingSource() throws IOException {
        final var input = """
            first: single
            second: [one, two, one]
            empty:
            empty-list: []
            """;
        Files.writeString(tempDir.resolve("mapping.yaml"), input);
        final var roots = List.of("items-map", "by-group");
        final var mapping = source(roots);

        final var generated = YamlResourceGenerator.generate(
            config(Map.of("mapping.yaml", mapping)), tempDir, resources());

        assertThat(generated.sources(), is(Map.of("data/mapping.yaml", mapping)));
        assertThat(Files.readString(tempDir.resolve("mapping.yaml")), is(input));
        final var output = resources().resolve("data/mapping.yaml");
        assertThat(Files.readAllLines(output).subList(0, 2), is(List.of("items-map:", "  by-group:")));
        assertThat(GeneratorData.readYamlData(output, roots), is(Map.of(
            "first", List.of("single"), "second", List.of("one", "two", "one"),
            "empty", List.of(), "empty-list", List.of())));
    }

    @Test
    void shouldBindGeneratedYamlUsingConfiguredKeys() throws IOException {
        Files.writeString(tempDir.resolve("mapping.yaml"), "first: [one, two]\nsecond: single\nempty:\n");
        YamlResourceGenerator.generate(
            config(Map.of("mapping.yaml", source(List.of("items-map", "by-group")))), tempDir, resources());
        final var propertySources = new YamlPropertySourceLoader().load(
            "generated", new FileSystemResource(resources().resolve("data/mapping.yaml")));
        final var binder = new Binder(ConfigurationPropertySources.from(propertySources));

        final var bound = binder.bind("items-map", Bindable.of(Items.class)).get();

        assertThat(bound.byGroup(), is(Map.of(
            "first", List.of("one", "two"), "second", List.of("single"), "empty", List.of())));
    }

    record Items(Map<String, List<String>> byGroup) {
    }

    @Test
    void shouldGenerateEveryConfiguredDocumentationDictionary() throws IOException {
        final var configFile = Path.of("src/main/resources/enum-generator-config.yaml");
        final var original = GeneratorData.readConfig(configFile);
        final var generated = YamlResourceGenerator.generate(original, configFile.getParent(), resources());
        final var mapper = new ObjectMapper(new YAMLFactory());

        assertThat(generated.sources().size(), is(7));
        for (final var entry : original.sources().entrySet()) {
            final var input = configFile.getParent().resolve(entry.getKey());
            final Map<String, Object> dictionary = mapper.readValue(Files.readString(input), new TypeReference<>() {});
            final var expected = new LinkedHashMap<String, List<String>>();
            dictionary.forEach((key, value) -> expected.put(key, value instanceof final String text
                ? List.of(text) : GeneratorData.extractValuesList(
                    new java.util.AbstractMap.SimpleEntry<>(key, value), key, input.toString())));
            final var resourceName = "data/" + input.getFileName();
            assertThat(GeneratorData.readYamlData(
                resources().resolve(resourceName), entry.getValue().rootKeys()), is(expected));
            assertThat(generated.sources().get(resourceName), is(entry.getValue()));
        }
        final var imports = new Properties();
        try (final var reader = Files.newBufferedReader(Path.of("src/main/resources/reference-data.properties"))) {
            imports.load(reader);
        }
        assertThat(imports.size(), is(generated.sources().size()));
        for (var index = 0; index < generated.sources().size(); index++) {
            final var location = imports.getProperty("spring.config.import[" + index + "]");
            assertThat(generated.sources().containsKey(location.replace("classpath:", "")), is(true));
        }
    }

    @Test
    void shouldRefreshOutputWhenDocumentationChanges() throws IOException {
        final var config = config(Map.of("mapping.yaml", source(List.of("items-map", "by-group"))));
        Files.writeString(tempDir.resolve("mapping.yaml"), "key: original\n");
        YamlResourceGenerator.generate(config, tempDir, resources());
        Files.writeString(tempDir.resolve("mapping.yaml"), "key: updated\n");

        YamlResourceGenerator.generate(config, tempDir, resources());

        assertThat(GeneratorData.readYamlData(resources().resolve("data/mapping.yaml"),
            List.of("items-map", "by-group")), is(Map.of("key", List.of("updated"))));
    }

    @Test
    void shouldRejectDuplicateOutputFilenames() throws IOException {
        Files.createDirectories(tempDir.resolve("first"));
        Files.createDirectories(tempDir.resolve("second"));
        Files.writeString(tempDir.resolve("first/mapping.yaml"), "key: value\n");
        Files.writeString(tempDir.resolve("second/mapping.yaml"), "key: value\n");
        final var mapping = source(List.of("items-map", "by-group"));
        final var config = config(Map.of("first/mapping.yaml", mapping, "second/mapping.yaml", mapping));
        final var resourcesDir = resources();

        final var exception = assertThrows(IllegalStateException.class, () -> YamlResourceGenerator.generate(
            config, tempDir, resourcesDir));

        assertThat(exception.getMessage(), containsString("Duplicate generated resource filename"));
    }

    @Test
    void shouldRejectInvalidRootKeys() {
        final var resourcesDir = resources();
        for (final var keys : List.of(List.<String>of(), List.of("one"), List.of("one", " "),
                List.of("one", "two", "three"))) {
            final var config = config(Map.of("mapping.yaml", source(keys)));
            assertThrows(IllegalStateException.class, () -> YamlResourceGenerator.generate(
                config, tempDir, resourcesDir));
        }
    }

    @Test
    void shouldRejectInvalidDictionaryValuesAndEmptyFiles() throws IOException {
        final var config = config(Map.of("mapping.yaml", source(List.of("items-map", "by-group"))));
        final var resourcesDir = resources();
        for (final var input : List.of("key: 123\n", "key: [valid, 123]\n", "key: {nested: value}\n", "{}\n")) {
            Files.writeString(tempDir.resolve("mapping.yaml"), input);
            assertThrows(IllegalStateException.class, () -> YamlResourceGenerator.generate(
                config, tempDir, resourcesDir));
        }
    }

    @Test
    void shouldFailWhenSourceIsMissing() {
        final var config = config(Map.of("missing.yaml", source(List.of("items-map", "by-group"))));
        final var resourcesDir = resources();
        assertThrows(NoSuchFileException.class, () -> YamlResourceGenerator.generate(
            config, tempDir, resourcesDir));
    }
}

package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnumGeneratorConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseCoverageTargetAndExclusionsWhenConfigured() throws IOException {
        final var config = GeneratorData.readConfig(Path.of("src/main/resources/enum-generator-config.yaml"));
        final var forms = config.sources().get("../../../docs/statement-matrix-docs/form-group-to-form.yaml");

        assertThat(forms.requireNonEmptyValuesIn(), is(List.of(
            new EnumGeneratorConfig.NonEmptyValuesRequirement(
                "../../../docs/statement-matrix-docs/form-group-to-presenter-types.yaml",
                List.of("unmarked", "exempt-form")))));
        assertThat(config.sources().values().stream()
            .filter(source -> !source.requireNonEmptyValuesIn().isEmpty()).count(), is(1L));
    }

    @Test
    void shouldDefaultRequirementsAndExclusionsToEmptyWhenOmitted() throws IOException {
        final var configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            output-package:
              enums: uk.test.enums
              bimaps: uk.test.bimaps
            sources:
              forms.yaml:
                root-keys: [forms, by-group]
                key-enum: Group
                value-enum: Form
                enums: {}
                require-non-empty-values-in:
                  - target: presenters.yaml
              presenters.yaml:
                root-keys: [presenters, by-group]
                key-enum: Group
                value-enum: Presenter
                enums: {}
            """);

        final var config = GeneratorData.readConfig(configFile);

        assertThat(config.sources().get("forms.yaml").requireNonEmptyValuesIn(), is(List.of(
            new EnumGeneratorConfig.NonEmptyValuesRequirement("presenters.yaml", List.of()))));
        assertThat(config.sources().get("presenters.yaml").requireNonEmptyValuesIn(), is(List.of()));
    }

    @Test
    void shouldPreserveEmptyRequirementsWhenUsingExistingSourceConstructor() {
        final var source = new EnumGeneratorConfig.Source(List.of("forms", "by-group"), "Group", "Form", Map.of());

        assertThat(source.requireNonEmptyValuesIn(), is(List.of()));
    }
}

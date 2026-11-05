package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class GeneratorValidatorCoverageTest {

    @TempDir
    Path tempDir;

    private static EnumGeneratorConfig.Source source(final String keyEnum,
            final List<EnumGeneratorConfig.NonEmptyValuesRequirement> requirements) {
        return new EnumGeneratorConfig.Source(List.of("mapping", "by-group"), keyEnum, "Item", Map.of(), requirements);
    }

    private static EnumGeneratorConfig.NonEmptyValuesRequirement requirement(final String target,
            final String... excludedKeys) {
        return new EnumGeneratorConfig.NonEmptyValuesRequirement(target, List.of(excludedKeys));
    }

    private static EnumGeneratorConfig config(final EnumGeneratorConfig.Source source,
            final EnumGeneratorConfig.Source target) {
        final var sources = new LinkedHashMap<String, EnumGeneratorConfig.Source>();
        sources.put("source.yaml", source);
        sources.put("target.yaml", target);
        return new EnumGeneratorConfig(new EnumGeneratorConfig.OutputPackage("uk.test.enums", "uk.test.bimaps"), sources);
    }

    private GeneratorValidator validator(final String sourceDictionary, final String targetDictionary,
            final EnumGeneratorConfig config) throws IOException {
        writeDictionary("source.yaml", sourceDictionary);
        if (targetDictionary != null) {
            writeDictionary("target.yaml", targetDictionary);
        }
        return new GeneratorValidator(config, tempDir, Map.of());
    }

    private void writeDictionary(final String filename, final String dictionary) throws IOException {
        Files.writeString(tempDir.resolve(filename), "mapping:\n  by-group:\n" + dictionary.indent(4));
    }

    private static EnumGeneratorConfig configured(final String... excludedKeys) {
        return config(source("Group", List.of(requirement("target.yaml", excludedKeys))), source("Group", List.of()));
    }

    @Test
    void shouldPassWhenEveryPopulatedSourceKeyHasTargetValues() throws IOException {
        final var validator = validator("first: [form1, form2]\nsecond: [form3]\nempty: []\n",
            "first: [presenter1]\nsecond: [presenter2]\n", configured());

        assertDoesNotThrow(validator::validateNonEmptyValuesInTargets);
    }

    @Test
    void shouldReportAllMissingAndEmptyMappingsInSourceOrderWhenCoverageFails() throws IOException {
        final var validator = validator("""
            missing: [form1, form2]
            empty: [form3]
            null-value: [form4]
            excluded: [form5]
            unpopulated: []
            covered: [form6]
            """, """
            empty: []
            null-value:
            covered: [presenter]
            """, configured("excluded"));

        final var exception = assertThrows(IllegalStateException.class, validator::validateNonEmptyValuesInTargets);

        assertThat(exception.getMessage(), containsString("source.yaml"));
        assertThat(exception.getMessage(), containsString("target.yaml"));
        assertThat(exception.getMessage(), containsString(
            "{missing=[form1, form2], empty=[form3], null-value=[form4]}"));
        assertThat(exception.getMessage(), not(containsString("form5")));
        assertThat(exception.getMessage(), not(containsString("unpopulated")));
        assertThat(exception.getMessage(), not(containsString("form6")));
    }

    @Test
    void shouldIgnoreEmptyMappingsWhenNoCoverageIsConfigured() throws IOException {
        final var validator = validator("group: [form]\n", "group: []\n",
            config(source("Group", List.of()), source("Group", List.of())));

        assertDoesNotThrow(validator::validateNonEmptyValuesInTargets);
    }

    @Test
    void shouldPassWhenAllUncoveredPopulatedKeysAreExcluded() throws IOException {
        final var validator = validator("missing: [form1]\nempty: [form2]\n", "empty: []\n",
            configured("missing", "empty"));

        assertDoesNotThrow(validator::validateNonEmptyValuesInTargets);
    }

    @Test
    void shouldKeepExclusionsSpecificToEachTarget() throws IOException {
        final var config = configured("group");
        config.sources().put("source.yaml", source("Group", List.of(
            requirement("target.yaml", "group"), requirement("other.yaml"))));
        config.sources().put("other.yaml", source("Group", List.of()));
        writeDictionary("other.yaml", "group: []\n");
        final var validator = validator("group: [form]\n", "group: []\n", config);

        final var exception = assertThrows(IllegalStateException.class, validator::validateNonEmptyValuesInTargets);

        assertThat(exception.getMessage(), containsString("target other.yaml"));
        assertThat(exception.getMessage(), containsString("group=[form]"));
    }

    @Test
    void shouldWarnWithoutFailingWhenExcludedSourceKeysAreMissingOrEmptyDespiteTargetCoverage() throws IOException {
        final var validator = validator("empty: []\npopulated: [form]\n",
            "missing: [presenter1]\nempty: [presenter2]\npopulated: [presenter3]\n",
            configured("missing", "empty", "populated"));
        final var logger = (Logger) LoggerFactory.getLogger("uk.gov");
        final var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertDoesNotThrow(validator::validateNonEmptyValuesInTargets);

            final var messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
            assertThat(messages.size(), is(2));
            assertThat(messages.get(0), containsString("[Warning] Coverage exclusion 'missing'"));
            assertThat(messages.get(1), containsString("[Warning] Coverage exclusion 'empty'"));
            assertThat(messages.get(0), containsString("may need updating if 'missing' is no longer needed."));
            assertThat(messages.get(1), containsString("may need updating if 'empty' is no longer needed."));
            for (final var message : messages) {
                assertThat(message, containsString("source.yaml"));
                assertThat(message, containsString("target.yaml"));
                assertThat(message, containsString("excluded-keys in enum-generator-config.yaml may need updating"));
            }
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void shouldIdentifyOnlyMissingAndEmptySourceKeysAsStaleExclusions() {
        final var stale = GeneratorValidator.staleExcludedKeys(
            Map.of("empty", List.of(), "populated", List.of("form")), List.of("missing", "empty", "populated"));

        assertThat(stale, is(Set.of("missing", "empty")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "unknown.yaml"})
    void shouldRejectInvalidTargetReferences(final String target) throws IOException {
        final var config = config(source("Group", List.of(requirement(target))), source("Group", List.of()));
        final var validator = validator("group: [form]\n", "group: [presenter]\n", config);

        final var exception = assertThrows(IllegalStateException.class, validator::validateNonEmptyValuesInTargets);

        assertThat(exception.getMessage(), containsString("source.yaml"));
        assertThat(exception.getMessage(), containsString("must identify an entry in sources"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "OtherGroup"})
    void shouldRejectMissingOrMismatchedSourceKeyEnums(final String keyEnum) throws IOException {
        final var config = config(source(keyEnum, List.of(requirement("target.yaml"))), source("Group", List.of()));
        final var validator = validator("group: [form]\n", "group: [presenter]\n", config);

        final var exception = assertThrows(IllegalStateException.class, validator::validateNonEmptyValuesInTargets);

        assertThat(exception.getMessage(), containsString("same non-blank key-enum"));
    }

    @Test
    void shouldRejectMissingTargetKeyEnumWhenSourceHasKeyEnum() throws IOException {
        final var validator = validator("group: [form]\n", "group: [presenter]\n",
            config(source("Group", List.of(requirement("target.yaml"))), source(null, List.of())));

        assertThrows(IllegalStateException.class, validator::validateNonEmptyValuesInTargets);
    }

    @Test
    void shouldRejectTwoMissingKeyEnumsInsteadOfTreatingThemAsMatching() throws IOException {
        final var validator = validator("group: [form]\n", "group: [presenter]\n",
            config(source(null, List.of(requirement("target.yaml"))), source(null, List.of())));

        assertThrows(IllegalStateException.class, validator::validateNonEmptyValuesInTargets);
    }

    @Test
    void shouldPropagateIOExceptionWhenConfiguredTargetFileIsMissing() throws IOException {
        final var validator = validator("group: [form]\n", null, configured());

        assertThrows(NoSuchFileException.class, validator::validateNonEmptyValuesInTargets);
    }
}

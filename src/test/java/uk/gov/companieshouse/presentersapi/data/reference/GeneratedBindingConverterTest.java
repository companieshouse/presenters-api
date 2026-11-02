package uk.gov.companieshouse.presentersapi.data.reference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.tools.ToolProvider;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.FileSystemResource;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;

class GeneratedBindingConverterTest {

    private static final String PACKAGE = "uk.test.generated";
    private static final String DICTIONARY = """
        firm-delivery:
          - 4.13(NI)
          - AD01
        individual-delivery:
          - 413(NI)
        """;

    @TempDir
    Path tempDir;

    @BeforeEach
    void generateAndCompileBindingClasses() throws Exception {
        Files.writeString(tempDir.resolve("forms.yaml"), DICTIONARY);
        Files.writeString(tempDir.resolve("config.yaml"), """
            output-package:
              enums: uk.test.generated.enums
              bimaps: uk.test.generated.bimaps
            sources:
              forms.yaml:
                root-keys: [form-types-map, by-form-group]
                key-enum: FormGroup
                value-enum: FormType
                enums:
                  FormGroup:
                    location: DICTIONARY_KEYS
                  FormType:
                    location: DICTIONARY_VALUES
                    validation:
                      uniqueness: VALUES_PER_KEY_SET
            """);
        final var sources = tempDir.resolve("sources");
        EnumBiMapGenerator.main(new String[] {tempDir.resolve("config.yaml").toString(),
            sources.toString(), tempDir.resolve("resources").toString()});
        Files.writeString(sources.resolve("uk/test/generated/BindingConfiguration.java"), """
            package uk.test.generated;

            import org.springframework.boot.context.properties.EnableConfigurationProperties;
            import org.springframework.context.annotation.ComponentScan;
            import org.springframework.context.annotation.Configuration;
            import uk.test.generated.bimaps.FormGroupFormTypeBiMap;
            import uk.test.generated.enums.FormTypeBindingConverter;

            @Configuration(proxyBeanMethods = false)
            @ComponentScan(basePackageClasses = FormTypeBindingConverter.class)
            @EnableConfigurationProperties(FormGroupFormTypeBiMap.class)
            public class BindingConfiguration {
            }
            """);
        final var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat("A JDK is required to compile generated sources", compiler, is(notNullValue()));
        try (final var manager = compiler.getStandardFileManager(null, null, null);
                final var files = Files.walk(sources)) {
            final var units = manager.getJavaFileObjectsFromPaths(
                files.filter(path -> path.toString().endsWith(".java")).toList());
            final var options = List.of("-parameters", "-classpath", System.getProperty("java.class.path"),
                "-d", Files.createDirectories(tempDir.resolve("classes")).toString());
            assertThat(compiler.getTask(null, manager, null, options, null, units).call(), is(true));
        }
    }

    @Test
    void shouldDiscoverConvertersAndBindDistinctFormCodesInBothDirections() throws Exception {
        try (final var loader = classLoader()) {
            final var formType = loader.loadClass(PACKAGE + ".enums.FormType");
            final var formGroup = loader.loadClass(PACKAGE + ".enums.FormGroup");
            final var bimapType = loader.loadClass(PACKAGE + ".bimaps.FormGroupFormTypeBiMap");
            final var dotted = formType.getField("_4_13_NI").get(null);
            final var undotted = formType.getField("_413_NI").get(null);
            final var standard = formType.getField("AD01").get(null);
            final var firm = formGroup.getField("FIRM_DELIVERY").get(null);
            final var individual = formGroup.getField("INDIVIDUAL_DELIVERY").get(null);
            final var forward = bimapType.getMethod("formTypesFor", formGroup);
            final var reverse = bimapType.getMethod("formGroupFor", formType);

            runner(loader).run(context -> {
                assertThat(context.getStartupFailure(), is(nullValue()));
                assertThat(context.getBeanNamesForType(loader.loadClass(PACKAGE + ".enums.FormTypeBindingConverter")).length,
                    is(1));
                assertThat(context.getBeanNamesForType(loader.loadClass(PACKAGE + ".enums.FormGroupBindingConverter")).length,
                    is(1));
                final var bimap = context.getBean(bimapType);
                assertThat(forward.invoke(bimap, firm), is(Set.of(dotted, standard)));
                assertThat(forward.invoke(bimap, individual), is(Set.of(undotted)));
                assertThat(reverse.invoke(bimap, dotted), is(firm));
                assertThat(reverse.invoke(bimap, undotted), is(individual));
                assertThat(formType.getMethod("code").invoke(dotted), is("4.13(NI)"));
                assertThat(formType.getMethod("code").invoke(undotted), is("413(NI)"));
            });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"4 13(NI)", "4.13(ni)", "unknown"})
    void shouldRejectInexactFormCodesWhenUsingConverterDirectly(final String code) throws Exception {
        try (final var loader = classLoader()) {
            final var converterType = loader.loadClass(PACKAGE + ".enums.FormTypeBindingConverter");
            final var converter = converterType.getConstructor().newInstance();

            final var failure = assertThrows(InvocationTargetException.class,
                () -> converterType.getMethod("convert", String.class).invoke(converter, code));
            assertInvalidCodeFailure(failure, FormTypeNotFoundException.class, "Form Type: " + code);
        }
    }

    @Test
    void shouldFailBindingForUnknownFormCodes() throws Exception {
        replaceResource("4.13(NI)", "unknown");

        try (final var loader = classLoader()) {
            runner(loader).run(context -> assertInvalidCodeFailure(context.getStartupFailure(),
                                                                   FormTypeNotFoundException.class,
                                                                   "Form Type: unknown"));
        }
    }

    @Test
    void shouldRejectUnknownMapKeys() throws Exception {
        replaceResource("firm-delivery:", "unknown-group:");

        try (final var loader = classLoader()) {
            runner(loader).run(context -> assertInvalidCodeFailure(context.getStartupFailure(),
                                                                   IllegalArgumentException.class,
                                                                   "Unknown FormGroup: unknown-group"));
        }
    }

    @Test
    void shouldPreserveSpringSupportForLiteralEnumConstantNames() throws Exception {
        replaceResource("4.13(NI)", "_4_13_NI");
        replaceResource("firm-delivery:", "FIRM_DELIVERY:");

        try (final var loader = classLoader()) {
            final var formType = loader.loadClass(PACKAGE + ".enums.FormType");
            final var formGroup = loader.loadClass(PACKAGE + ".enums.FormGroup");
            final var bimapType = loader.loadClass(PACKAGE + ".bimaps.FormGroupFormTypeBiMap");

            runner(loader).run(context -> {
                assertThat(context.getStartupFailure(), is(nullValue()));
                final var value = formType.getField("_4_13_NI").get(null);
                final var group = formGroup.getField("FIRM_DELIVERY").get(null);
                assertThat(bimapType.getMethod("formGroupFor", formType).invoke(context.getBean(bimapType), value),
                    is(group));
            });
        }
    }

    private static void assertInvalidCodeFailure(final Throwable failure,
            final Class<? extends Throwable> exceptionType, final String message) {
        assertThat(failure, is(notNullValue()));
        assertThat(ExceptionUtils.getRootCause(failure), is(instanceOf(exceptionType)));
        assertThat(ExceptionUtils.getStackTrace(failure), containsString(message));
    }

    private void replaceResource(final String original, final String replacement) throws IOException {
        final var resource = tempDir.resolve("resources/data/forms.yaml");
        Files.writeString(resource, Files.readString(resource).replace(original, replacement));
    }

    private URLClassLoader classLoader() throws IOException {
        return new URLClassLoader(new URL[] {tempDir.resolve("classes").toUri().toURL()}, getClass().getClassLoader());
    }

    private ApplicationContextRunner runner(final ClassLoader loader) throws ClassNotFoundException, IOException {
        final var propertySources = new YamlPropertySourceLoader().load(
            "forms", new FileSystemResource(tempDir.resolve("resources/data/forms.yaml")));
        return new ApplicationContextRunner()
            .withClassLoader(loader)
            .withUserConfiguration(loader.loadClass(PACKAGE + ".BindingConfiguration"))
            .withInitializer(context -> propertySources.forEach(
                source -> context.getEnvironment().getPropertySources().addLast(source)));
    }
}

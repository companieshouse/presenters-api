package uk.gov.companieshouse.presentersapi.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;

@ExtendWith(MockitoExtension.class)
class PresenterApiFeatureFlagTest {

    @Mock
    private PresenterDetailsService presenterDetailsService;

    @Mock
    private PresenterTypesService presenterTypesService;

    @Mock
    private PresenterValidationService presenterValidationService;

    // Create a WebApplicationContextRunner to test the application context with different configurations.
    // Spring will create the controller beans if the feature flag is set to true, otherwise they will not be created.
    // This ensures that the @ConditionalOnProperty annotation on the controller classes is genuinely tested.
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withUserConfiguration(ControllerConfiguration.class)
        .withBean(PresenterDetailsService.class, () -> presenterDetailsService)
        .withBean(PresenterTypesService.class, () -> presenterTypesService)
        .withBean(PresenterValidationService.class, () -> presenterValidationService);

    @Test
    void shouldRegisterPresenterEndpointsWhenEnvironmentFlagIsTrue() throws IOException {
        withEnvironmentFlag("true").run(context -> {
            assertThat(context.getBeansOfType(PresenterDetailsControllerImpl.class).size(), is(1));
            assertThat(context.getBeansOfType(PresenterTypesControllerImpl.class).size(), is(1));
            assertThat(context.getBeansOfType(PresenterValidationControllerImpl.class).size(), is(1));

            final var handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
            final var paths = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .sorted()
                .toList();

            assertThat(paths, is(List.of(
                "/presenters/presenter-details",
                "/presenters/presenter-types",
                "/presenters/validation"
            )));
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"false", "not-true"})
    void shouldReturnNotFoundWhenEnvironmentFlagIsNotTrue(final String flag) throws IOException {
        withEnvironmentFlag(flag).run(context -> {
            assertThat(context.getBeansOfType(PresenterDetailsControllerImpl.class).isEmpty(), is(true));
            assertThat(context.getBeansOfType(PresenterTypesControllerImpl.class).isEmpty(), is(true));
            assertThat(context.getBeansOfType(PresenterValidationControllerImpl.class).isEmpty(), is(true));

            final var mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
            mockMvc.perform(get("/presenters/presenter-details")).andExpect(status().isNotFound());
            mockMvc.perform(get("/presenters/presenter-types")).andExpect(status().isNotFound());
            mockMvc.perform(post("/presenters/validation")
                .contentType("application/json").content("{}")).andExpect(status().isNotFound());
        });
    }

    @Test
    void shouldNotRegisterPresenterEndpointsWhenPropertyIsMissing() {
        contextRunner.run(context -> {
            assertThat(context.getBeansOfType(PresenterDetailsControllerImpl.class).isEmpty(), is(true));
            assertThat(context.getBeansOfType(PresenterTypesControllerImpl.class).isEmpty(), is(true));
            assertThat(context.getBeansOfType(PresenterValidationControllerImpl.class).isEmpty(), is(true));
        });
    }

    // Read the production application.properties file and add the feature flag to the environment for the context runner.
    // Then replace the system environment property source with the modified environment for the context runner.
    // This happens before the context is refreshed, so that the controllers are created or not created based on the feature flag.
    private WebApplicationContextRunner withEnvironmentFlag(final String flag) throws IOException {
        final var applicationProperties = new ResourcePropertySource("file:src/main/resources/application.properties");
        return contextRunner.withInitializer(context -> {
            final var propertySources = context.getEnvironment().getPropertySources();
            propertySources.remove("systemEnvironment");
            propertySources.addLast(applicationProperties);
            final Map<String, Object> environment = flag == null
                ? Map.of() : Map.of("FEATURE_FLAG_PRESENTERS_230926", flag);
            propertySources.addFirst(new SystemEnvironmentPropertySource("featureFlagEnvironment", environment));
        });
    }

    // Supply just the MVC infrastructure and the controllers to the context runner, so that we can test the @ConditionalOnProperty annotation on the controllers.
    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({PresenterDetailsControllerImpl.class, PresenterTypesControllerImpl.class,
        PresenterValidationControllerImpl.class})
    static class ControllerConfiguration {
    }
}

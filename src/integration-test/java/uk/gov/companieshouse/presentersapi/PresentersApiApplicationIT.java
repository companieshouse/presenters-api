package uk.gov.companieshouse.presentersapi;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.api.interceptor.MappablePermissionsInterceptor;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.interceptor.RequestLoggingInterceptor;

@SpringBootTest
class PresentersApiApplicationIT {

    @Autowired
    private RequestLoggingInterceptor requestLoggingInterceptor;

    @Autowired
    private InternalUserInterceptor internalUserInterceptor;

    @Autowired
    private TokenPermissionsInterceptor tokenPermissionsInterceptor;

    @Autowired
    @Qualifier("chsUserProfileInterceptor")
    private MappablePermissionsInterceptor userProfileInterceptor;

    @Autowired
    @Qualifier("chsAcspAssociationsInterceptor")
    private MappablePermissionsInterceptor acspAssociationsInterceptor;

    @Autowired
    private Logger logger;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void shouldInjectLoggingConfigLoggerWhenRequestLoggingInterceptorCreated() {
        final var injectedLogger = ReflectionTestUtils.getField(requestLoggingInterceptor, "logger");

        assertThat("Interceptor should use the singleton Logger bean from LoggingConfig",
            injectedLogger, is(sameInstance(logger)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/presenters/presenter-type", "/presenters/presenter-details"})
    void shouldApplyLoggingAndPermissionInterceptorsInOrderWhenPresenterRequestHandled(
        final String path
    ) throws Exception {
        final var request = new MockHttpServletRequest("GET", path);

        assertThat("Presenter requests should use logging and permission beans in order, without internal-user validation",
            applicationInterceptorsFor(request), is(List.of(
                requestLoggingInterceptor,
                tokenPermissionsInterceptor,
                userProfileInterceptor,
                acspAssociationsInterceptor
            )));
    }

    @Test
    void shouldApplyLoggingBeforeInternalUserInterceptorToValidationRequests() throws Exception {
        final var request = new MockHttpServletRequest("POST", "/presenters/validation");
        request.setContentType("application/json");

        assertThat("Validation requests should use logging then internal-user validation, without permission interceptors",
            applicationInterceptorsFor(request), is(List.of(requestLoggingInterceptor, internalUserInterceptor)));
    }

    private List<HandlerInterceptor> applicationInterceptorsFor(final MockHttpServletRequest request) throws Exception {
        final var handlerChain = handlerMapping.getHandler(request);

        assertThat("Controller handler should be found for " + request.getRequestURI(), handlerChain, is(notNullValue()));
        return handlerChain.getInterceptorList().stream()
            .filter(interceptor -> interceptor instanceof RequestLoggingInterceptor
                || interceptor instanceof InternalUserInterceptor
                || interceptor instanceof TokenPermissionsInterceptor
                || interceptor instanceof MappablePermissionsInterceptor)
            .toList();
    }
}

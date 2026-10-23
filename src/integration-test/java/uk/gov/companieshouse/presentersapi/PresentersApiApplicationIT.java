package uk.gov.companieshouse.presentersapi;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isA;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.interceptor.RequestLoggingInterceptor;

@SpringBootTest
class PresentersApiApplicationIT {

    @Autowired
    private RequestLoggingInterceptor requestLoggingInterceptor;

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

    @Test
    void shouldApplyRequestLoggingInterceptorWhenControllerRequestHandled() throws Exception {
        final var request = new MockHttpServletRequest("GET", "/presenters/presenter-type");

        final var handlerChain = handlerMapping.getHandler(request);

        assertThat("Controller handler should be found for the request", handlerChain, is(notNullValue()));
        assertThat("Registered interceptor should be the RequestLoggingInterceptor bean",
            handlerChain.getInterceptorList(), hasItem(sameInstance((HandlerInterceptor) requestLoggingInterceptor)));
        assertThat("Internal user interceptor should not apply to non-validation requests",
            handlerChain.getInterceptorList(), not(hasItem(isA(InternalUserInterceptor.class))));
    }

    @Test
    void shouldApplyLoggingBeforeInternalUserInterceptorToValidationRequests() throws Exception {
        final var request = new MockHttpServletRequest("POST", "/presenters/validation");
        request.setContentType("application/json");

        final var handlerChain = handlerMapping.getHandler(request);

        assertThat("Validation controller handler should be found", handlerChain, is(notNullValue()));
        final var interceptors = handlerChain.getInterceptorList();
        assertThat("Request logging interceptor should apply to validation requests",
            interceptors, hasItem(sameInstance((HandlerInterceptor) requestLoggingInterceptor)));
        assertThat("Internal user interceptor should apply to validation requests",
            interceptors, hasItem(isA(InternalUserInterceptor.class)));

        final var loggingIndex = interceptors.indexOf(requestLoggingInterceptor);
        final var internalUserIndex = IntStream.range(0, interceptors.size())
            .filter(index -> interceptors.get(index) instanceof InternalUserInterceptor)
            .findFirst()
            .orElseThrow();
        assertThat("Request logging interceptor should run before internal user validation",
            internalUserIndex, is(greaterThan(loggingIndex)));
    }
}

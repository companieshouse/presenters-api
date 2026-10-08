package uk.gov.companieshouse.presentersapi;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.interceptor.RequestLoggingInterceptor;

@SpringBootTest
class PresentersApiApplicationTest {

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
    }
}

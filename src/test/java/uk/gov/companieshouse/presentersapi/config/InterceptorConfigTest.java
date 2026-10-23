package uk.gov.companieshouse.presentersapi.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.interceptor.RequestLoggingInterceptor;

@ExtendWith(MockitoExtension.class)
class InterceptorConfigTest {

    @Mock
    private Logger logger;

    @Mock
    private InterceptorRegistry registry;

    @Mock
    private InterceptorRegistration loggingRegistration;

    @Mock
    private InterceptorRegistration internalUserRegistration;

    private InterceptorConfig interceptorConfig;

    @BeforeEach
    void setUp() {
        interceptorConfig = new InterceptorConfig(logger);
    }

    @Test
    void shouldRegisterRequestLoggingInterceptorBeforeInternalUserInterceptor() {
        when(registry.addInterceptor(any(HandlerInterceptor.class)))
            .thenReturn(loggingRegistration, internalUserRegistration);
        when(loggingRegistration.addPathPatterns("/presenters/**"))
            .thenReturn(loggingRegistration);
        when(internalUserRegistration.addPathPatterns("/presenters/validation"))
            .thenReturn(internalUserRegistration);

        interceptorConfig.addInterceptors(registry);

        final var order = inOrder(registry);
        order.verify(registry).addInterceptor(any(RequestLoggingInterceptor.class));
        order.verify(registry).addInterceptor(any(InternalUserInterceptor.class));

        verify(loggingRegistration).addPathPatterns("/presenters/**");
        verify(loggingRegistration).order(0);
        verifyNoMoreInteractions(loggingRegistration);
        verify(internalUserRegistration).addPathPatterns("/presenters/validation");
        verify(internalUserRegistration).order(1);
        verifyNoMoreInteractions(internalUserRegistration);
    }

    @Test
    void shouldCreateRequestLoggingInterceptorWhenBeanRequested() {
        final var interceptor = interceptorConfig.requestLoggingInterceptor();

        assertThat(interceptor, is(isA(RequestLoggingInterceptor.class)));
    }
}

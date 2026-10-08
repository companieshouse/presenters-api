package uk.gov.companieshouse.presentersapi.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.interceptor.RequestLoggingInterceptor;

@ExtendWith(MockitoExtension.class)
class InterceptorConfigTest {

    @Mock
    private Logger logger;

    @Mock
    private InterceptorRegistry registry;

    @Mock
    private InterceptorRegistration registration;

    private InterceptorConfig interceptorConfig;

    @BeforeEach
    void setUp() {
        interceptorConfig = new InterceptorConfig(logger);
    }

    @Test
    void shouldRegisterRequestLoggingInterceptorLastWhenAddingInterceptors() {
        when(registry.addInterceptor(any(RequestLoggingInterceptor.class))).thenReturn(registration);

        interceptorConfig.addInterceptors(registry);

        verify(registration).order(Integer.MAX_VALUE);
        verifyNoMoreInteractions(registration);
    }

    @Test
    void shouldCreateRequestLoggingInterceptorWhenBeanRequested() {
        final var interceptor = interceptorConfig.requestLoggingInterceptor();

        assertThat(interceptor, is(isA(RequestLoggingInterceptor.class)));
    }
}

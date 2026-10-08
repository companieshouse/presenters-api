package uk.gov.companieshouse.presentersapi.interceptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.util.LogContext;

@ExtendWith(MockitoExtension.class)
class RequestLoggingInterceptorTest {

    private static final Object HANDLER = new Object();

    @Mock
    private Logger logger;

    private RequestLoggingInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RequestLoggingInterceptor(logger);
        request = new MockHttpServletRequest("GET", "/presenters/test");
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldLogStartOfRequestAndContinueWhenPreHandleCalled() {
        final var result = interceptor.preHandle(request, response, HANDLER);

        assertThat("Request processing should continue to the handler", result, is(true));
        verify(logger).infoStartOfRequest(any(LogContext.class));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void shouldLogEndOfRequestOnlyWhenCompletedWithoutException() {
        interceptor.preHandle(request, response, HANDLER);
        response.setStatus(HttpStatus.OK.value());

        interceptor.afterCompletion(request, response, HANDLER, null);

        verify(logger).infoStartOfRequest(any(LogContext.class));
        verify(logger).infoEndOfRequest(any(LogContext.class), eq(HttpStatus.OK.value()), anyLong());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void shouldLogErrorAndEndOfRequestWhenCompletedWithException() {
        final var exception = new IllegalStateException("test");
        interceptor.preHandle(request, response, HANDLER);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        interceptor.afterCompletion(request, response, HANDLER, exception);

        verify(logger).infoStartOfRequest(any(LogContext.class));
        verify(logger).errorRequest(request, "Unhandled exception processing request", exception);
        verify(logger).infoEndOfRequest(any(LogContext.class), eq(HttpStatus.INTERNAL_SERVER_ERROR.value()),
            anyLong());
        verifyNoMoreInteractions(logger);
    }
}

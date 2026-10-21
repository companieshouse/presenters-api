package uk.gov.companieshouse.presentersapi.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.exception.InternalServerError;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotValidException;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {

    @Mock
    private Logger logger;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(logger);
    }

    @Test
    @DisplayName("should handle bad request exceptions")
    void shouldHandleBadRequestExceptions() {
        ApiErrors result = handler.handleBadRequestException(
            new PresenterTypeNotValidException("invalid presenter type"),
            webRequest("/presenters/validate"));

        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
        verify(logger, times(1)).error(anyString(), any(Exception.class), anyMap());
    }

    @Test
    @DisplayName("should handle not found exceptions")
    void shouldHandleNotFoundExceptions() {
        ApiErrors result = handler.handleNotFoundException(
            new PresenterNotFoundException("presenter not found"),
            webRequest("/presenters/not-found"));

        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
        verify(logger, times(1)).error(anyString(), any(Exception.class), anyMap());
    }

    @Test
    @DisplayName("should handle internal server error exception")
    void shouldHandleInternalServerErrorException() {
        ApiErrors result = handler.handleAllUncaughtException(
            new InternalServerError("boom"),
            webRequest("/presenters/internal"));

        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
        verify(logger, times(1)).error(anyString(), any(Exception.class), anyMap());
    }

    private ServletWebRequest webRequest(final String uri) {
        return new ServletWebRequest(new MockHttpServletRequest("GET", uri));
    }
}
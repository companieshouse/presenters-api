package uk.gov.companieshouse.presentersapi.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import uk.gov.companieshouse.api.error.ApiError;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.InternalServerError;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotExemptException;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotValidAcspException;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotVerifiedException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotValidException;
import uk.gov.companieshouse.presentersapi.exception.PresenterValidationException;
import uk.gov.companieshouse.presentersapi.exception.StatementNotFoundException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler{

    private final Logger logger;

    public GlobalExceptionHandler(Logger logger) {
        this.logger = logger;
    }

    @ExceptionHandler({
        PresenterTypeNotValidException.class,
        PresenterValidationException.class,
        PresenterNotExemptException.class,
        PresenterNotVerifiedException.class,
        PresenterNotValidAcspException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiErrors handleBadRequestException(final Exception ex,
                                                final WebRequest request) {
        final var error = new ApiError(ex.getMessage(),
            getRequestURI(request),
            LocationType.RESOURCE.getValue(), ErrorType.VALIDATION.getType());

        Optional.ofNullable(ex.getMessage())
            .ifPresent(m -> error.addErrorValue("Resource not found: ", m));

        final var errorList = List.of(error);
        logError(logger, request, ex.getMessage(), ex, errorList);
        return new ApiErrors(errorList);
    }

    @ExceptionHandler({
        PresenterNotFoundException.class,
        FormTypeNotFoundException.class,
        StatementNotFoundException.class,
        PresenterTypeNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ApiErrors handleNotFoundException (final Exception ex,
                                             final WebRequest request) {
        final var error = new ApiError(ex.getMessage(),
            getRequestURI(request),
            LocationType.RESOURCE.getValue(), ErrorType.VALIDATION.getType());

        Optional.ofNullable(ex.getMessage())
            .ifPresent(m -> error.addErrorValue("Resource not found: ", m));

        final var errorList = List.of(error);
        logError(logger, request, ex.getMessage(), ex, errorList);
        return new ApiErrors(errorList);
    }

    @ExceptionHandler(InternalServerError.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiErrors handleAllUncaughtException(final Exception ex,
                                                final WebRequest request) {
        final var errorList = List.of(createApiServiceError(ex, request, logger));
        logError(logger, request, "Internal error", ex, errorList);
        return new ApiErrors(errorList);
    }

    /**
     * Creates a generic API error for service-level exceptions, including error location and type.
     * Adds extra logging for certain IllegalArgumentExceptions.
     *
     * @param ex the exception
     * @param request the web request
     * @param logger the logger
     * @return an ApiError representing the service error
     */
    private static ApiError createApiServiceError(final Exception ex,
                                                  final WebRequest request,
                                                  final Logger logger) {
        final var error = new ApiError("Service Unavailable: {error}",
            getRequestURI(request), LocationType.RESOURCE.getValue(),
            ErrorType.SERVICE.getType());

        Optional.ofNullable(ex)
            .filter(IllegalArgumentException.class::isInstance)
            .map(Throwable::getCause)
            .map(Throwable::getMessage)
            .map(m -> m.contains("expected numeric type"))
            .ifPresent(c -> logError(logger, request, "A dependent CHS service may be unavailable", ex));

        String errorMessage = "Internal server error";
        if (ex != null && ex.getMessage() != null && !ex.getMessage().trim().isEmpty()) {
            errorMessage = ex.getMessage();
        }

        error.addErrorValue("error", errorMessage);

        return error;
    }

    /**
     * Extracts the request URI from the WebRequest, or returns null if unavailable.
     *
     * @param request the web request
     * @return the request URI string, or null
     */
    private static String getRequestURI(final WebRequest request) {
        // resolveReference("request") preferred over getRequest() because the latter method is
        // final and cannot be stubbed with Mockito
        return Optional.ofNullable((HttpServletRequest) request.resolveReference("request"))
            .map(HttpServletRequest::getRequestURI)
            .orElse(null);
    }

    /**
     * Logs an error with the given message and exception, using the provided logger and request context.
     *
     * @param chLogger the logger
     * @param request the web request
     * @param msg the log message
     * @param ex the exception
     */
    private static void logError(final Logger chLogger, final WebRequest request, final String msg,
                                 final Exception ex) {
        logError(chLogger, request, msg, ex, null);
    }

    /**
     * Logs an error with optional ApiError details, using the provided logger and request context.
     *
     * @param chLogger the logger
     * @param request the web request
     * @param msg the log message
     * @param ex the exception
     * @param apiErrorList optional list of ApiError objects for context
     */
    private static void logError(final Logger chLogger, final WebRequest request, final String msg,
                                 final Exception ex,
                                 @Nullable final List<ApiError> apiErrorList) {
        final Map<String, Object> logMap = new HashMap<>();
        final var servletRequest = ((ServletWebRequest) request).getRequest();
        logMap.put("path", servletRequest.getRequestURI());
        logMap.put("method", servletRequest.getMethod());
        if (apiErrorList != null) {
            logMap.put("errors", apiErrorList);
        }
        chLogger.error(msg, ex, logMap);
    }
}

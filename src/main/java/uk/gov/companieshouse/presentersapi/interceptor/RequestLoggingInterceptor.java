package uk.gov.companieshouse.presentersapi.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.util.RequestLogger;

/**
 * Interceptor for logging the start and end of HTTP request processing.
 * <p>
 * Uses CH structured logging to log request lifecycle events for monitoring and debugging.
 * </p>
 * <p>Overrides afterCompletion rather than postHandle so that unhandled exceptions are logged
 * and the end of request processing is recorded regardless.</p>
 */
public class RequestLoggingInterceptor implements HandlerInterceptor, RequestLogger {

    private final Logger logger;

    public RequestLoggingInterceptor(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response,
        final Object handler) {
        logStartRequestProcessing(request, logger);
        return true;
    }

    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response,
        final Object handler, final Exception ex) {
        if (ex != null) {
            logger.errorRequest(request, "Unhandled exception processing request", ex);
        }
        logEndRequestProcessing(request, response, logger);
    }

}
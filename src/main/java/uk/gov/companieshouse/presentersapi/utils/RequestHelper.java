package uk.gov.companieshouse.presentersapi.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;

public final class RequestHelper {
    
    private static final String ERIC_ACCESS_TOKEN_HEADER = "ERIC-Access-Token";

    private RequestHelper() {
    }

    /**
     * Retrieves the HTTP request bound to the current thread. This allows request details to be read by
     * controllers implementing generated API interfaces, whose method signatures do not expose the request.
     *
     * @return the current HTTP servlet request
     * @throws IllegalStateException if no request is bound to the current thread
     */
    private static HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    /**
     * Retrieves the CHS User ID from the request bound to the current thread.
     *
     * @return the CHS User ID
     * @throws ResponseStatusException with status 401 if the user is not an OAuth2 user or the user ID is blank
     * @throws IllegalStateException   if no request is bound to the current thread
     */
    public static String getChsUserId() {
        HttpServletRequest request = currentRequest();
        String userId = AuthorisationUtil.getAuthorisedIdentity(request);

        if (!AuthorisationUtil.isOauth2User(request) || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "An OAuth2 user is required");
        }

        return userId;
    }

    public static String requireHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing required header: " + headerName);
        }

        return value;
    }

    /**
     * Retrieves the ERIC passthrough token from the request bound to the current thread.
     * Enforces that the ERIC passthrough token header is present and not blank.
     *
     * @return the ERIC passthrough token
     * @throws ResponseStatusException with status 400 if the header is missing or blank
     * @throws IllegalStateException   if no request is bound to the current thread
     */
    public static String requirePassthroughToken() {
        return requireHeader(currentRequest(), ERIC_ACCESS_TOKEN_HEADER);
    }
}

package uk.gov.companieshouse.presentersapi.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;

public final class UserHelper {

    private UserHelper() {
    }

    /**
     * NOTE: This helper is intended to allow the CHS User ID to be retrieved from the request.
     * Currently, the CHS User ID is passed in as a query parameter, but is it more secure to
     * retrieve it from the OAuth2 token instead?
     * Example used would be:
     * <pre>{@code
     * String userId = UserHelper.getChsUserId(request);
     * String passthroughToken = UserHelper.requireHeader(request, ERIC_ACCESS_TOKEN_HEADER);
     * Identity identity = identityVerificationService.getIdentityByUserId(userId, passthroughToken);
     * }</pre>
     */
    public static String getChsUserId(HttpServletRequest request) {
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
}

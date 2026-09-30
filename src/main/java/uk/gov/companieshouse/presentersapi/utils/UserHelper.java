package uk.gov.companieshouse.presentersapi.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;

public final class UserHelper {

    private UserHelper() {
    }

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

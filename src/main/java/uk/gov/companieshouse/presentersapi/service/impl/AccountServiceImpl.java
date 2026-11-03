package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.client.PrivateApiClientFactory;
import uk.gov.companieshouse.presentersapi.service.AccountService;

@Service
public class AccountServiceImpl implements AccountService {

    private static final String USER_ID = "user_id";
    private static final String USER_DETAILS_URI_TEMPLATE = "/users/%s";

    private final Logger logger;
    private final PrivateApiClientFactory apiClientFactory;

    public AccountServiceImpl(final Logger logger, final PrivateApiClientFactory apiClientFactory) {
        this.logger = logger;
        this.apiClientFactory = apiClientFactory;
    }


    public User getUserById(final String userId) throws URIValidationException {
        final Map<String, Object> logMap = Map.of(USER_ID, userId);
        try {
            final User user = apiClientFactory.createInternal()
                    .privateAccountsUserResourceHandler()
                    .getUserDetails(USER_DETAILS_URI_TEMPLATE.formatted(userId))
                    .execute()
                    .getData();
            logger.infoContext(userId, "Retrieved account user details", logMap);
            return user;
        } catch (final URIValidationException exception) {
            logger.errorContext(userId, "Invalid accounts user URI", exception, logMap);
            throw exception;
        } catch (final IllegalStateException exception) {
            logger.errorContext(userId, "Accounts API client configuration is unavailable", exception, logMap);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Accounts API is unavailable", exception);
         } catch (final ApiErrorResponseException exception) {
				 logger.errorContext(userId, "Accounts API returned an error", exception, logMap);
				 throw new ResponseStatusException(
					 toHttpStatusCode(exception, userId),
					 "Accounts API request failed",
					 exception);
        }
	}

    @Override
    public String getUserEmailByUserId(final String userId)
            throws URIValidationException {
        final Map<String, Object> logMap = Map.of(USER_ID, userId);
        final User user = getUserById(userId);
        if (user == null || user.getEmail().isBlank()) {
            logger.error("No account email found for user", logMap);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No account email found for user");
        }
        return user.getEmail();
    }

    private HttpStatusCode toHttpStatusCode(final ApiErrorResponseException exception, final String userId) {
        try {
            return HttpStatusCode.valueOf(exception.getStatusCode());
        } catch (final IllegalArgumentException invalidStatusCodeException) {
            logger.errorContext(userId,
                    "Accounts API returned an invalid HTTP status code",
                    invalidStatusCodeException,
                    Map.of(USER_ID, userId, "status_code", exception.getStatusCode()));
            return HttpStatus.BAD_GATEWAY;
        }
    }

}

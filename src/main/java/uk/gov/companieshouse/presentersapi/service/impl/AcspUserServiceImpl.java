package uk.gov.companieshouse.presentersapi.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.client.PrivateApiClientFactory;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;

@Service
public class AcspUserServiceImpl implements AcspUserService {

    private final PrivateApiClientFactory apiClientFactory;
    private final Logger logger;

    public AcspUserServiceImpl(final PrivateApiClientFactory apiClientFactory, final Logger logger) {
        this.apiClientFactory = apiClientFactory;
        this.logger = logger;
    }

    @Override
    public Optional<AcspMembership> getAcspMembershipForUser(final String userId, final String passthroughToken) {
        final Map<String, Object> logMap = Map.of("user_id", userId);
        try {
            final AcspMembershipsList memberships = getUserAcspMembershipsExcludingRemoved(passthroughToken);
            logger.infoContext(userId, "Retrieved ACSP memberships for user", logMap);

            if (memberships == null || memberships.getItems() == null) {
                return Optional.empty();
            }
            return memberships.getItems().stream().findFirst();
        } catch (ApiErrorResponseException exception) {
            logger.errorContext(userId, "ACSP manage users API returned an error", exception, logMap);
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(exception.getStatusCode()),
                    "ACSP manage users API request failed", exception);
        } catch (URIValidationException exception) {
            logger.errorContext(userId, "Invalid ACSP manage users API URI", exception, logMap);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid ACSP manage users API URI", exception);
        } catch (IOException exception) {
            logger.errorContext(userId, "Unable to retrieve ACSP memberships", exception, logMap);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "ACSP manage users API is unavailable", exception);
        }
    }

    private AcspMembershipsList getUserAcspMembershipsExcludingRemoved(
        final String passthroughToken) throws URIValidationException, IOException  {
            return apiClientFactory.create(passthroughToken)
                .privateAcspManageUsersApiResourceHandler()
                .getUserAcspMemberships(false)
                .execute()
                .getData();
    }
}

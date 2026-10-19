package uk.gov.companieshouse.presentersapi.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.client.PrivateApiClientFactory;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;

@Service
public class AcspUserServiceImpl implements AcspUserService {

    private static final boolean INCLUDE_REMOVED_MEMBERSHIPS = false;

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
            final AcspMembershipsList memberships = apiClientFactory.create(passthroughToken)
                    .privateAcspManageUsersApiResourceHandler()
                    .getUserAcspMemberships(INCLUDE_REMOVED_MEMBERSHIPS)
                    .execute()
                    .getData();
            logger.infoContext(userId, "Retrieved ACSP memberships for user", logMap);

            if (memberships == null || memberships.getItems() == null) {
                return Optional.empty();
            }
            return memberships.getItems().stream().findFirst();
        } catch (URIValidationException | IOException exception) {
            logger.errorContext(userId, "Error retrieving ACSP memberships", exception, logMap);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "unhandled error occurred while retrieving ACSP memberships", exception);
        }
    }
}
package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.identityverification.model.Uvid;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.client.InternalApiClientFactory;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;

@Service
public class IndividualUserServiceImpl implements IndividualUserService {

    private static final String IDENTITY_BY_USER_ID_URI = "/verification/identities";
    private static final String UVIDS_BY_IDENTITY_ID_URI = "/verification/identities/%s/uvids";

    private final InternalApiClientFactory apiClientFactory;
    private final Logger logger;

    public IndividualUserServiceImpl(InternalApiClientFactory apiClientFactory, Logger logger) {
        this.apiClientFactory = apiClientFactory;
        this.logger = logger;
    }

    @Override
    public Identity getIdentityByUserId(String userId) throws URIValidationException {
        Map<String, Object> logMap = Map.of("user_id", userId);
        try {
            InternalApiClient apiClient = apiClientFactory.create();
            Identity identity = apiClient.privateIdentityVerificationResourceHandler()
                    .findIdentityByUserId(IDENTITY_BY_USER_ID_URI, userId)
                    .execute()
                    .getData();
            logger.infoContext(userId, "Retrieved identity-verification details", logMap);
            return identity;
        } catch (URIValidationException exception) {
            logger.errorContext(userId, "Invalid identity-verification URI", exception, logMap);
            throw exception;
        } catch (ApiErrorResponseException exception) {
            logger.errorContext(userId, "Identity-verification API returned an error", exception, logMap);
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(exception.getStatusCode()),
                    "Identity-verification API request failed",
                    exception);
        }
    }

    @Override
    public Uvid getActiveUvidByIdentityId(String identityId)
            throws URIValidationException {
        Map<String, Object> logMap = Map.of("identity_id", identityId);
        try {
            InternalApiClient apiClient = apiClientFactory.create();
            List<Uvid> activeUvids = apiClient.privateIdentityVerificationResourceHandler()
                    .findUvidsByIdentityId(UVIDS_BY_IDENTITY_ID_URI.formatted(identityId), true)
                    .execute()
                    .getData()
                    .getData();

            if (activeUvids == null || activeUvids.isEmpty()) {
                logger.error("No active UVID was found for the identity", logMap);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active UVID was found for the identity");
            }
            if (activeUvids.size() > 1) {
                logger.error("More than one active UVID was found for the identity", logMap);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "More than one active UVID was found for the identity");
            }

            logger.infoContext(identityId, "Retrieved active UVID for identity", logMap);
            return activeUvids.getFirst();
        } catch (URIValidationException exception) {
            logger.errorContext(identityId, "Invalid UVID lookup URI", exception, logMap);
            throw exception;
        } catch (ApiErrorResponseException exception) {
            logger.errorContext(identityId, "Identity-verification API returned an error", exception, logMap);
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(exception.getStatusCode()),
                    "Identity-verification API request failed",
                    exception);
        }
    }
}

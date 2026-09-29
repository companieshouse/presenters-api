package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.identityverification.model.Uvid;

/**
 * Retrieves verified identity details through the private API SDK.
 */
public interface IdentityVerificationService {

    Identity getIdentityByUserId(String userId, String passthroughToken) throws URIValidationException;

    Uvid getActiveUvidByIdentityId(String identityId, String passthroughToken) throws URIValidationException;
}

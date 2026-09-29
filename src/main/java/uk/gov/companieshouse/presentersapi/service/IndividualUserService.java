package uk.gov.companieshouse.presentersapi.service;

import jakarta.servlet.http.HttpServletRequest;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.Identity;

/**
 * Retrieves identity-verification details for the signed-in CHS user.
 */
public interface IndividualUserService {

    Identity getIdentityVerificationDetails(HttpServletRequest request) throws URIValidationException;
}

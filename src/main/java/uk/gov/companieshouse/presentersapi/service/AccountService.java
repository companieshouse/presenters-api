package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.model.accounts.Accounts;

import java.io.IOException;
import java.util.Optional;

public interface AccountService {
	/**
	 * @param userId           the CHS user ID, used for logging
	 * @param passthroughToken the ERIC passthrough token identifying the user to the downstream API
	 * @return an Optional containing the Identity if the user is an account holder, or an empty Optional otherwise
	 * @throws org.springframework.web.server.ResponseStatusException with status 500 if the memberships cannot be
	 *     retrieved
	 */
	String getUserEmailByUserId(String userId, String passthroughToken) throws IOException, URIValidationException;
}

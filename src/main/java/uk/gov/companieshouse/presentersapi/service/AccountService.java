package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.handler.exception.URIValidationException;


import java.io.IOException;

public interface AccountService {
	/**
	 * @param userId           the CHS user ID, used for logging
	 * @return an Optional containing the Identity if the user is an account holder, or an empty Optional otherwise
	 * @throws org.springframework.web.server.ResponseStatusException with status 500 if the exemptUser cannot be
	 *     retrieved
	 */
	String getUserEmailByUserId(String userId) throws IOException, URIValidationException;
}

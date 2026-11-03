package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.handler.exception.URIValidationException;


public interface AccountService {
	/**
	 * @param userId the CHS user ID used to retrieve the account user and for logging
	 * @return the account user's email address
	 * @throws URIValidationException if the generated user URI is invalid
	 */
	String getUserEmailByUserId(String userId)  throws URIValidationException;
}

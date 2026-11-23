package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.service.ServiceResult;

public interface PresenterDetailsService {

    /**
     * Return a list of statements and presenter information.
     *
     * @param userId           the userId
     * @param formType         the form type
     * @param presenterType    the presenter type
     * @param passthroughToken the ERIC passthrough token identifying the user to downstream APIs, required for ACSP
     * @return list of statements and presenter information
     */
    ServiceResult<PresenterDetails> getPresenterDetails(
        String userId, String formType, String presenterType, String passthroughToken);
}

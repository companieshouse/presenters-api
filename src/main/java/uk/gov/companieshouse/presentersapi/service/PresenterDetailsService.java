package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.presenters.model.PresenterDetails;

public interface PresenterDetailsService {

    /**
     * Return a list of statements and presenter information.
     *
     * @param userId   the userId
     * @param formType the form type
     * @param presenterType the presenter type
     * @return list of statements and presenter information
     */
    PresenterDetails getPresenterDetails(String userId, String formType, String presenterType);
}

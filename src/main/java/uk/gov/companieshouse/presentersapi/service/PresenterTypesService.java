package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.presenters.model.PresenterTypes;

public interface PresenterTypesService {

    /**
     * Get the available presenter types for a given user and form type.
     *
     * @param userId   the userId
     * @param formType the form type
     * @return the available presenter types
     */
    PresenterTypes getPresenterTypes(String userId, String formType);
}

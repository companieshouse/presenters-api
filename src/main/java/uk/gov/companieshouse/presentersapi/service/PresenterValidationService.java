package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;

public interface PresenterValidationService {

    /**
     * Validate the presenter details for a given user and form type.
     *
     * @param validationRequest the validation request containing user ID, form type, presenter type, presenter name, and presenter email
     * @return the validation response
     */
    ValidationResponse validatePresenterDetails(ValidationRequest validationRequest);
}

package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.service.ServiceResult;

public interface PresenterValidationService {

    /**
     * Determines the {@code permittedPresenterTypes} for the supplied form, then checks that at least one
     * permitted presenter exactly matches both the submitted data types and one complete valid statement
     * combination. Returns the first validation failure.
     *
     * @param validationRequest the pre-validated form type, data types and statement identifiers
     * @return a found result containing a VALID response when a presenter matches; otherwise an unprocessable
     *     result containing an error. If {@code permittedPresenterTypes} is empty, the error is located at
     *     {@code $.form_type}; data and statement mismatches are located at {@code $.data_types} and
     *     {@code $.statement_ids}, respectively.
     */
    ServiceResult<ValidationResponse> validatePresenterDetails(final ValidationRequest validationRequest);
}

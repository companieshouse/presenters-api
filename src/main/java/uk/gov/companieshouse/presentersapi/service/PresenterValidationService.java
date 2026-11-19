package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.service.ServiceResult;

public interface PresenterValidationService {

    /**
     * Validates that at least one presenter permitted for the supplied form exactly matches both the submitted
     * data types and one complete valid statement combination. Checks permitted presenters, data types, then
     * statements, returning the first failure.
     *
     * @param validationRequest the pre-validated form type, data types and statement identifiers
     * @return a found result containing a VALID response when a presenter matches; otherwise an unprocessable
     *     result containing errors. Data and statement mismatches are located at {@code data_types} and
     *     {@code statement_ids} in the request body; no permitted presenters produces a service error.
     */
    ServiceResult<ValidationResponse> validatePresenterDetails(final ValidationRequest validationRequest);
}

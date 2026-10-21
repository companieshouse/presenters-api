package uk.gov.companieshouse.presentersapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterValidationRequestApi;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotExemptException;
import uk.gov.companieshouse.presentersapi.exception.PresenterValidationException;
import uk.gov.companieshouse.presentersapi.exception.StatementNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;

@RestController
public class PresenterValidationControllerImpl implements PresenterValidationRequestApi {

    private final PresenterValidationService presenterValidationService;

    public PresenterValidationControllerImpl(final PresenterValidationService presenterValidationService) {
        this.presenterValidationService = presenterValidationService;
    }

    @Override
    public ResponseEntity<ValidationResponse> validatePresenterDetails(@Valid ValidationRequest validationRequest) {

        // Mock scenarios to be removed
        applyMockScenarios(validationRequest);

        ValidationResponse validationResponse =
            presenterValidationService.validatePresenterDetails(validationRequest);

        return ResponseEntity.ok(validationResponse);
    }

    private static void applyMockScenarios(ValidationRequest validationRequest) {

        if (validationRequest.getStatementIds().contains("statementNotFound")) {
            throw new StatementNotFoundException("Statement not valid statement");
        } else if (validationRequest.getStatementIds().contains("presenterValidation")) {
            throw new PresenterValidationException("Input validation fails");
        } else if (validationRequest.getStatementIds().contains("presenterNotExempt")) {
            throw new PresenterNotExemptException("Presenter is not on the exemption list but the exempt statement has been given.");
        }
    }
}

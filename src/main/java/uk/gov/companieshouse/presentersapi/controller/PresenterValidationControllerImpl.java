package uk.gov.companieshouse.presentersapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterValidationRequestApi;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;

@RestController
public class PresenterValidationControllerImpl implements PresenterValidationRequestApi {

    private final PresenterValidationService presenterValidationService;

    public PresenterValidationControllerImpl(final PresenterValidationService presenterValidationService) {
        this.presenterValidationService = presenterValidationService;
    }

    @Override
    public ResponseEntity<ValidationResponse> validatePresenterDetails(@Valid ValidationRequest validationRequest) {

        ValidationResponse validationResponse =
            presenterValidationService.validatePresenterDetails(validationRequest);

        return ResponseEntity.ok(validationResponse);
    }
}

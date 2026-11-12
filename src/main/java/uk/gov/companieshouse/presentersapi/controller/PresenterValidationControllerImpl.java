package uk.gov.companieshouse.presentersapi.controller;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterValidationRequestApi;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotExemptException;
import uk.gov.companieshouse.presentersapi.exception.PresenterValidationException;
import uk.gov.companieshouse.presentersapi.exception.StatementNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@RestController
@ConditionalOnProperty(prefix = "feature", name = "endpoints-enabled", havingValue = "true")
public class PresenterValidationControllerImpl implements PresenterValidationRequestApi {

    private final PresenterValidationService presenterValidationService;
    private final PluggableResponseEntityFactory responseEntityFactory;

    public PresenterValidationControllerImpl(final PresenterValidationService presenterValidationService, final PluggableResponseEntityFactory responseEntityFactory) {
        this.presenterValidationService = presenterValidationService;
        this.responseEntityFactory = responseEntityFactory;
    }

    @Override
    public ResponseEntity<ChResponseBody<ValidationResponse>> validatePresenterDetails(@Valid final ValidationRequest validationRequest) {

        // Mock scenarios to be removed
        applyMockScenarios(validationRequest);

        final var validationResponse = presenterValidationService.validatePresenterDetails(validationRequest);

        return responseEntityFactory.createResponse(ServiceResult.found(validationResponse));
    }

    private static void applyMockScenarios(final ValidationRequest validationRequest) {

        if (validationRequest.getStatementIds().contains("statementNotFound")) {
            throw new StatementNotFoundException("Statement not valid statement");
        } else if (validationRequest.getStatementIds().contains("presenterValidation")) {
            throw new PresenterValidationException("Input validation fails");
        } else if (validationRequest.getStatementIds().contains("presenterNotExempt")) {
            throw new PresenterNotExemptException("Presenter is not on the exemption list but the exempt statement has been given.");
        }
    }
}

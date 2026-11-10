package uk.gov.companieshouse.presentersapi.controller;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterValidationRequestApi;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@RestController
@ConditionalOnProperty(prefix = "feature", name = "endpoints-enabled", havingValue = "true")
public class PresenterValidationControllerImpl implements PresenterValidationRequestApi {

    public static final String STATEMENT_IDS = "statement_ids";
    private final PresenterValidationService presenterValidationService;
    private final PluggableResponseEntityFactory responseEntityFactory;

    public PresenterValidationControllerImpl(final PresenterValidationService presenterValidationService, final PluggableResponseEntityFactory responseEntityFactory) {
        this.presenterValidationService = presenterValidationService;
        this.responseEntityFactory = responseEntityFactory;
    }

    @Override
    public ResponseEntity<ChResponseBody<ValidationResponse>> validatePresenterDetails(@Valid final ValidationRequest validationRequest) {

        final var mockScenarioResult = applyMockScenarios(validationRequest);
        if (ServiceResultStatus.NOT_FOUND != mockScenarioResult.getStatus()) {
            return responseEntityFactory.createResponse(mockScenarioResult);
        }

        final var validationResponse =
            presenterValidationService.validatePresenterDetails(validationRequest);

        return responseEntityFactory.createResponse(validationResponse);
    }

    private static ServiceResult<ValidationResponse> applyMockScenarios(final ValidationRequest validationRequest) {

        if (validationRequest.getStatementIds().contains("statementNotFound")) {
            return ServiceResult.invalid(new Errors(Err.invalidBodyBuilderWithLocation(STATEMENT_IDS)
                .withError("Statement not valid statement")
                .build()));
        } else if (validationRequest.getStatementIds().contains("presenterValidation")) {
            return ServiceResult.invalid(new Errors(Err.invalidBodyBuilderWithLocation(STATEMENT_IDS)
                .withError("Input validation fails")
                .build()));
        } else if (validationRequest.getStatementIds().contains("presenterNotExempt")) {
            return ServiceResult.invalid(new Errors(Err.invalidBodyBuilderWithLocation(STATEMENT_IDS)
                .withError("Presenter is not on the exemption list but the exempt statement has been given.")
                .build()));
        }
        return ServiceResult.notFound();
    }
}

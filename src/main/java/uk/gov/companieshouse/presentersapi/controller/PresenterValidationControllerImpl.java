package uk.gov.companieshouse.presentersapi.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterValidationRequestApi;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotExemptException;
import uk.gov.companieshouse.presentersapi.exception.PresenterValidationException;
import uk.gov.companieshouse.presentersapi.exception.StatementNotFoundException;
import uk.gov.companieshouse.presentersapi.guard.FormTypeGuard;
import uk.gov.companieshouse.presentersapi.guard.Guarded;
import uk.gov.companieshouse.presentersapi.guard.RequestListGuard;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@RestController
@ConditionalOnProperty(prefix = "feature", name = "endpoints-enabled", havingValue = "true")
public class PresenterValidationControllerImpl implements PresenterValidationRequestApi {

    private final PresenterValidationService presenterValidationService;
    private final PluggableResponseEntityFactory responseEntityFactory;
    private final FormTypeGuard formTypeGuard;
    private final Logger logger;

    public PresenterValidationControllerImpl(final PresenterValidationService presenterValidationService,
        final PluggableResponseEntityFactory responseEntityFactory, final FormTypeGuard formTypeGuard, final Logger logger) {
        this.presenterValidationService = presenterValidationService;
        this.responseEntityFactory = responseEntityFactory;
        this.formTypeGuard = formTypeGuard;
        this.logger = logger;
    }

    @Override
    public ResponseEntity<ChResponseBody<ValidationResponse>> validatePresenterDetails(@Valid final ValidationRequest validationRequest) {

        final var formType = formTypeGuard.fromBody(validationRequest.getFormType());
        final var statements = RequestListGuard.guardStatements(validationRequest.getStatementIds());
        final var dataTypes = RequestListGuard.guardDataObjectTypes(validationRequest.getDataTypes());
        final var errors = Guarded.combinedErrors(formType, statements, dataTypes);

        if (errors.hasErrors()) {
            logger.debugContext("PresenterValidationControllerImpl",
                                "Validation errors found",
                                Map.of("body", validationRequest, "errors", errors));
            return responseEntityFactory.createResponse(ServiceResult.invalid(errors));
        }

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

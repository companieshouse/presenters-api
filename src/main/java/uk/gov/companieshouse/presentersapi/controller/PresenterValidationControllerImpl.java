package uk.gov.companieshouse.presentersapi.controller;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterValidationRequestApi;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
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

    public PresenterValidationControllerImpl(final PresenterValidationService presenterValidationService,
        final PluggableResponseEntityFactory responseEntityFactory, final FormTypeGuard formTypeGuard) {
        this.presenterValidationService = presenterValidationService;
        this.responseEntityFactory = responseEntityFactory;
        this.formTypeGuard = formTypeGuard;
    }

    @Override
    public ResponseEntity<ChResponseBody<ValidationResponse>> validatePresenterDetails(@Valid final ValidationRequest validationRequest) {

        final var formType = formTypeGuard.fromBody(validationRequest.getFormType());
        final var statements = RequestListGuard.guardStatements(validationRequest.getStatementIds());
        final var dataTypes = RequestListGuard.guardDataObjectTypes(validationRequest.getDataTypes());
        final var errors = Guarded.combinedErrors(formType, statements, dataTypes);

        if (errors.hasErrors()) {
            return responseEntityFactory.createResponse(ServiceResult.invalid(errors));
        }

        final var validationResult = presenterValidationService.validatePresenterDetails(validationRequest);

        return responseEntityFactory.createResponse(validationResult);
    }

}

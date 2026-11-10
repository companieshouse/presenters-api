package uk.gov.companieshouse.presentersapi.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterTypesRequestApi;
import uk.gov.companieshouse.api.presenters.model.PresenterTypes;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.InternalServerError;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;
import uk.gov.companieshouse.presentersapi.utils.RequestHelper;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@RestController
@ConditionalOnProperty(prefix = "feature", name = "endpoints-enabled", havingValue = "true")
public class PresenterTypesControllerImpl implements PresenterTypesRequestApi {

    private final PresenterTypesService presenterTypesService;
    private final PluggableResponseEntityFactory responseEntityFactory;

    public PresenterTypesControllerImpl(final PresenterTypesService presenterTypesService,
        final PluggableResponseEntityFactory responseEntityFactory) {
        this.presenterTypesService = presenterTypesService;
        this.responseEntityFactory = responseEntityFactory;
    }

    @Override
    public ResponseEntity<ChResponseBody<PresenterTypes>> getPresenterTypes(String formType) {

        final var passthroughToken = RequestHelper.requirePassthroughToken();

        // Mock scenarios to be removed
        final var mockScenariosResult = applyMockScenarios(null, formType);
        if (ServiceResultStatus.NOT_FOUND != mockScenariosResult.getStatus()) { // A mock scenario, return early response
            return responseEntityFactory.createResponse(mockScenariosResult);
        }

        final var presenterTypesServiceResult = presenterTypesService.getPresenterTypes(null, formType, passthroughToken);

        if (ServiceResultStatus.FOUND == presenterTypesServiceResult.getStatus()) {
            final var presenterTypes = new PresenterTypes();

            presenterTypes.setTypes(presenterTypesServiceResult.getData().stream().map(PresenterType::code).toList());

            return responseEntityFactory.createResponse(new ServiceResult<>(ServiceResultStatus.FOUND, presenterTypes));
        }
        else {
            return responseEntityFactory.createResponse(new ServiceResult<>(presenterTypesServiceResult.getStatus(),
                                                                            presenterTypesServiceResult.getErrors()));
        }
    }

    private static ServiceResult<PresenterTypes> applyMockScenarios(String userId, String formType) {

        if ("formTypeNotFound".equals(formType)) {
            return ServiceResult.invalid(new Errors(Err
                                                        .invalidBodyBuilderWithLocation("form_type")
                                                        .withError("Form Type not supported")
                                                        .addErrorValue("form_type", formType)
                                                        .build()));
        }
        else if ("presenterNotFound".equals(userId)) {
            throw new PresenterNotFoundException("Presenter not found with UserId: %s".formatted(userId));
        }
        else if ("internalServerError".equals(userId)) {
            throw new InternalServerError("An unexpected error occurred while processing the request.");
        }
        return ServiceResult.notFound();
    }
}

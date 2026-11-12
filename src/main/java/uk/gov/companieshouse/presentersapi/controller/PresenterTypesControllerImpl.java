package uk.gov.companieshouse.presentersapi.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterTypesRequestApi;
import uk.gov.companieshouse.api.presenters.model.PresenterTypes;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;
import uk.gov.companieshouse.presentersapi.utils.RequestHelper;
import uk.gov.companieshouse.service.ServiceResult;
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
    public ResponseEntity<ChResponseBody<PresenterTypes>> getPresenterTypes(final String formType) {

        final var userId = RequestHelper.getChsUserId();
        final var passthroughToken = RequestHelper.requirePassthroughToken();

        // Mock scenarios to be removed
        applyMockScenarios(formType);

        final var presenterTypes = new PresenterTypes();

        presenterTypes.setTypes(presenterTypesService
                                    .getPresenterTypes(userId, formType, passthroughToken)
                                    .stream()
                                    .map(PresenterType::code)
                                    .toList());
        return responseEntityFactory.createResponse(ServiceResult.found(presenterTypes)); // Only sunny day response for now.
    }

    private static void applyMockScenarios(final String formType) {

        if ("formTypeNotFound".equals(formType)) {
            throw new FormTypeNotFoundException("Form Type: %s".formatted(formType));
        }
    }
}

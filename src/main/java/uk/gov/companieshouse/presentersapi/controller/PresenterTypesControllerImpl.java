package uk.gov.companieshouse.presentersapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterTypeRequestApi;
import uk.gov.companieshouse.api.presenters.model.PresenterTypes;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.InternalServerError;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@RestController
public class PresenterTypesControllerImpl implements PresenterTypeRequestApi {

    private final PresenterTypesService presenterTypesService;

    public PresenterTypesControllerImpl(final PresenterTypesService presenterTypesService) {
        this.presenterTypesService = presenterTypesService;
    }

    @Override
    public ResponseEntity<PresenterTypes> getPresenterTypes(String userId, String formType) {

        // Mock scenarios to be removed
        applyMockScenarios(userId, formType);

        final var presenterTypes = new PresenterTypes();
        presenterTypes.setTypes(presenterTypesService.getPresenterTypes(userId, formType).stream()
            .map(PresenterType::code)
            .toList());
        return ResponseEntity.ok(presenterTypes);
    }

    private static void applyMockScenarios(String userId, String formType) {

        if ("formTypeNotFound".equals(formType)) {
            throw new FormTypeNotFoundException("Form Type: %s".formatted(formType));
        } else if ("presenterNotFound".equals(userId)) {
            throw new PresenterNotFoundException("Presenter not found with UserId: %s".formatted(userId));
        } else if ("internalServerError".equals(userId)) {
            throw new InternalServerError("An unexpected error occurred while processing the request.");
        }
    }
}

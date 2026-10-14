package uk.gov.companieshouse.presentersapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterTypeRequestApi;
import uk.gov.companieshouse.api.presenters.model.PresenterTypes;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@RestController
public class PresenterTypesControllerImpl implements PresenterTypeRequestApi {

    private final PresenterTypesService presenterTypesService;

    public PresenterTypesControllerImpl(final PresenterTypesService presenterTypesService) {
        this.presenterTypesService = presenterTypesService;
    }

    @Override
    public ResponseEntity<PresenterTypes> getPresenterTypes(String userId, String formType) {
        final var presenterTypes = new PresenterTypes();
        presenterTypes.setTypes(presenterTypesService.getPresenterTypes(userId, formType).stream()
            .map(PresenterType::code)
            .toList());
        return ResponseEntity.ok(presenterTypes);
    }
}

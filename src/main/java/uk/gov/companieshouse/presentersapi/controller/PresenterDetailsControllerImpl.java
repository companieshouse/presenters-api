package uk.gov.companieshouse.presentersapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterDetailsRequestApi;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;

@RestController
public class PresenterDetailsControllerImpl implements PresenterDetailsRequestApi {

    private final PresenterDetailsService presenterDetailsService;

    public PresenterDetailsControllerImpl(final PresenterDetailsService presenterDetailsService) {
        this.presenterDetailsService = presenterDetailsService;
    }

    @Override
    public ResponseEntity<PresenterDetails> getPresenterDetails(String userId, String formType, String presenterType) {

        PresenterDetails presenterDetails = presenterDetailsService.getPresenterDetails(userId, formType, presenterType);

        return ResponseEntity.ok(presenterDetails);
    }
}

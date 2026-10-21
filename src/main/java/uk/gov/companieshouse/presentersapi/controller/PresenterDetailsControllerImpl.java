package uk.gov.companieshouse.presentersapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterDetailsRequestApi;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotValidAcspException;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotVerifiedException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotValidException;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;

@RestController
public class PresenterDetailsControllerImpl implements PresenterDetailsRequestApi {

    private final PresenterDetailsService presenterDetailsService;

    public PresenterDetailsControllerImpl(final PresenterDetailsService presenterDetailsService) {
        this.presenterDetailsService = presenterDetailsService;
    }

    @Override
    public ResponseEntity<PresenterDetails> getPresenterDetails(String userId, String formType, String presenterType) {

        // Mock scenarios to be removed
        applyMockScenarios(presenterType);

        PresenterDetails presenterDetails = presenterDetailsService.getPresenterDetails(userId, formType, presenterType);

        return ResponseEntity.ok(presenterDetails);
    }

    private static void applyMockScenarios(String presenterType) {

        if ("presenterTypeNotValid".equals(presenterType)) {
            throw new PresenterTypeNotValidException("Presenter Type is not valid for Form Type");
        } else if ("presenterTypeNotVerified".equals(presenterType)) {
            throw new PresenterNotVerifiedException("User has not completed their identity verification");
        } else if ("presenterNotValidAcsp".equals(presenterType)) {
            throw new PresenterNotValidAcspException("User is not associated with an ACSP");
        } else if ("presenterNotFound".equals(presenterType)) {
            throw new PresenterNotFoundException("User not found");
        } else if ("presenterTypeNotFound".equals(presenterType)) {
            throw new PresenterTypeNotFoundException("presenter type not found");
        }
    }
}

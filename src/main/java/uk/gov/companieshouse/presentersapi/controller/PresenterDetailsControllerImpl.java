package uk.gov.companieshouse.presentersapi.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import uk.gov.companieshouse.presentersapi.utils.RequestHelper;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@RestController
@ConditionalOnProperty(prefix = "feature", name = "endpoints-enabled", havingValue = "true")
public class PresenterDetailsControllerImpl implements PresenterDetailsRequestApi {

    private final PresenterDetailsService presenterDetailsService;
    private final PluggableResponseEntityFactory responseEntityFactory;

    public PresenterDetailsControllerImpl(final PresenterDetailsService presenterDetailsService, final PluggableResponseEntityFactory responseEntityFactory) {
        this.presenterDetailsService = presenterDetailsService;
        this.responseEntityFactory = responseEntityFactory;
    }

    @Override
    public ResponseEntity<ChResponseBody<PresenterDetails>> getPresenterDetails(final String formType, final String presenterType) {

        final var userId = RequestHelper.getChsUserId();

        // Mock scenarios to be removed
        applyMockScenarios(presenterType);

        final var presenterDetails = presenterDetailsService.getPresenterDetails(userId, formType, presenterType);

        return responseEntityFactory.createResponse(ServiceResult.found(presenterDetails));
    }

    private static void applyMockScenarios(final String presenterType) {

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

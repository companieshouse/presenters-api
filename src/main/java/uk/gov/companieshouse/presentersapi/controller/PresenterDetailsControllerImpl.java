package uk.gov.companieshouse.presentersapi.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.presenters.api.PresenterDetailsRequestApi;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@RestController
@ConditionalOnProperty(prefix = "feature", name = "endpoints-enabled", havingValue = "true")
public class PresenterDetailsControllerImpl implements PresenterDetailsRequestApi {

    public static final String PRESENTER_TYPE = "presenter_type";
    private final PresenterDetailsService presenterDetailsService;
    private final PluggableResponseEntityFactory responseEntityFactory;

    public PresenterDetailsControllerImpl(final PresenterDetailsService presenterDetailsService, final PluggableResponseEntityFactory responseEntityFactory) {
        this.presenterDetailsService = presenterDetailsService;
        this.responseEntityFactory = responseEntityFactory;
    }

    @Override
    public ResponseEntity<ChResponseBody<PresenterDetails>> getPresenterDetails(String formType, String presenterType) {

        final var mockScenarioResult = applyMockScenarios(presenterType);
        if (ServiceResultStatus.NOT_FOUND != mockScenarioResult.getStatus()) { // A mock scenario, return early response
            return responseEntityFactory.createResponse(mockScenarioResult);
        }

        final var presenterDetails = presenterDetailsService.getPresenterDetails(null, formType, presenterType);

        return responseEntityFactory.createResponse(presenterDetails);
    }

    private static ServiceResult<PresenterDetails> applyMockScenarios(String presenterType) {

        if ("presenterTypeNotValid".equals(presenterType)) {
            return ServiceResult.invalid(new Errors(Err
                                                        .invalidBodyBuilderWithLocation(PRESENTER_TYPE)
                                                        .withError("Presenter Type not supported")
                                                        .addErrorValue(PRESENTER_TYPE, presenterType)
                                                        .build()));
        } else if ("presenterTypeNotVerified".equals(presenterType)) {
            return ServiceResult.invalid(new Errors(Err
                                                        .invalidBodyBuilderWithLocation("user_identity_verification")
                                                        .withError("User identity verification not completed")
                                                        .build()));
        } else if ("presenterNotValidAcsp".equals(presenterType)) {
            return ServiceResult.invalid(new Errors(Err
                                                        .invalidBodyBuilderWithLocation("ascsp_association")
                                                        .withError("User is not associated with an ACSP")
                                                        .build()));
        } else if ("presenterNotFound".equals(presenterType)) {
            return ServiceResult.invalid(new Errors(Err
                                                        .invalidBodyBuilderWithLocation("user_not_found")
                                                        .withError("User not found")
                                                        .build()));
        } else if ("presenterTypeNotFound".equals(presenterType)) {
            // redundant with presenterTypeNotValid?
            return ServiceResult.invalid(new Errors(Err
                                                        .invalidBodyBuilderWithLocation(PRESENTER_TYPE)
                                                        .withError("Presenter Type not supported")
                                                        .addErrorValue(PRESENTER_TYPE, presenterType)
                                                        .build()));
        }
        return ServiceResult.notFound();
    }
}

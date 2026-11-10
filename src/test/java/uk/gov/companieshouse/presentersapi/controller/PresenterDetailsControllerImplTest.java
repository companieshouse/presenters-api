package uk.gov.companieshouse.presentersapi.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.presenters.model.Exempt;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterDetailsControllerImpl tests")
class PresenterDetailsControllerImplTest {

    @Mock
    private PresenterDetailsService presenterDetailsService;

    @Mock
    private PluggableResponseEntityFactory responseEntityFactory;

    @InjectMocks
    private PresenterDetailsControllerImpl controller;

    private PresenterDetails testPresenterDetails;

    @BeforeEach
    void setUp() {
        final var testExempt = new Exempt();
        testExempt.setOrganisationName("Test Organisation");
        testExempt.setName("Test Presenter");
        testExempt.setEmail("test@example.com");

        testPresenterDetails = new PresenterDetails();
        testPresenterDetails.setStatements(List.of("exempt-statement", "delivery-on-behalf-of-firm"));
        testPresenterDetails.setExempt(testExempt);
    }

    @Test
    @DisplayName("should return presenter details from the service result")
    void shouldReturnPresenterDetailsFromServiceResult() {
        final var formType = "form-abc";
        final var presenterType = "officer-employee";
        final ResponseEntity<ChResponseBody<PresenterDetails>> expectedResponse = ResponseEntity.ok().build();

        when(presenterDetailsService.getPresenterDetails(null, formType, presenterType))
            .thenReturn(ServiceResult.found(testPresenterDetails));
        when(responseEntityFactory.<PresenterDetails>createResponse(any())).thenReturn(expectedResponse);

        final var actualResponse = controller.getPresenterDetails(formType, presenterType);

        assertThat(actualResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(actualResponse, is(expectedResponse));
        verify(presenterDetailsService).getPresenterDetails(null, formType, presenterType);
        verify(responseEntityFactory).createResponse(argThat((ServiceResult<PresenterDetails> result) ->
            result.getStatus() == ServiceResultStatus.FOUND
                && result.getData().getStatements().equals(List.of("exempt-statement", "delivery-on-behalf-of-firm"))
                && result.getData().getExempt().getOrganisationName().equals("Test Organisation")));
    }

    @Test
    @DisplayName("should return an invalid response for an unsupported presenter type")
    void shouldReturnInvalidResponseForUnsupportedPresenterType() {
        final var error = Err.invalidBodyBuilderWithLocation("presenter_type")
            .withError("Presenter Type not supported")
            .addErrorValue("presenter_type", "presenterTypeNotValid")
            .build();

        assertInvalidMockScenario("presenterTypeNotValid", error);
    }

    @Test
    @DisplayName("should return an invalid response when identity verification is incomplete")
    void shouldReturnInvalidResponseWhenIdentityVerificationIsIncomplete() {
        final var error = Err.invalidBodyBuilderWithLocation("user_identity_verification")
            .withError("User identity verification not completed")
            .build();

        assertInvalidMockScenario("presenterTypeNotVerified", error);
    }

    @Test
    @DisplayName("should return an invalid response when the user is not associated with an ACSP")
    void shouldReturnInvalidResponseWhenUserIsNotAssociatedWithAcsp() {
        final var error = Err.invalidBodyBuilderWithLocation("ascsp_association")
            .withError("User is not associated with an ACSP")
            .build();

        assertInvalidMockScenario("presenterNotValidAcsp", error);
    }

    @Test
    @DisplayName("should return an invalid response when the user is not found")
    void shouldReturnInvalidResponseWhenUserIsNotFound() {
        final var error = Err.invalidBodyBuilderWithLocation("user_not_found")
            .withError("User not found")
            .build();

        assertInvalidMockScenario("presenterNotFound", error);
    }

    @Test
    @DisplayName("should return an invalid response for a presenter type that is not found")
    void shouldReturnInvalidResponseForPresenterTypeThatIsNotFound() {
        final var error = Err.invalidBodyBuilderWithLocation("presenter_type")
            .withError("Presenter Type not supported")
            .addErrorValue("presenter_type", "presenterTypeNotFound")
            .build();

        assertInvalidMockScenario("presenterTypeNotFound", error);
    }

    private void assertInvalidMockScenario(final String presenterType, final Err expectedError) {
        final ResponseEntity<ChResponseBody<PresenterDetails>> expectedResponse = ResponseEntity.badRequest().build();

        when(responseEntityFactory.<PresenterDetails>createResponse(any())).thenReturn(expectedResponse);

        final var actualResponse = controller.getPresenterDetails("form-abc", presenterType);

        assertThat(actualResponse, is(expectedResponse));
        verifyNoInteractions(presenterDetailsService);
        verify(responseEntityFactory).createResponse(argThat((ServiceResult<PresenterDetails> result) ->
            result.getErrors().containsError(expectedError)));
    }
}

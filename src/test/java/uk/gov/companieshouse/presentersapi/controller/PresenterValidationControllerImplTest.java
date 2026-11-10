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
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterValidationControllerImpl tests")
class PresenterValidationControllerImplTest {

    @Mock
    private PresenterValidationService presenterValidationService;

    @Mock
    private PluggableResponseEntityFactory responseEntityFactory;

    @InjectMocks
    private PresenterValidationControllerImpl controller;

    private ValidationRequest testValidationRequest;
    private ValidationResponse testValidationResponse;

    @BeforeEach
    void setUp() {
        testValidationRequest = new ValidationRequest();
        testValidationRequest.setStatementIds(List.of("statement-ok"));
        testValidationResponse = new ValidationResponse();
        testValidationResponse.setResult(ValidationResponse.ResultEnum.VALID);
    }

    @Test
    @DisplayName("should return the validation response from the service result")
    void shouldReturnValidationResponseFromServiceResult() {
        final ResponseEntity<ChResponseBody<ValidationResponse>> expectedResponse = ResponseEntity.ok().build();

        when(presenterValidationService.validatePresenterDetails(testValidationRequest))
            .thenReturn(ServiceResult.found(testValidationResponse));
        when(responseEntityFactory.<ValidationResponse>createResponse(any())).thenReturn(expectedResponse);

        final var actualResponse = controller.validatePresenterDetails(testValidationRequest);

        assertThat(actualResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(actualResponse, is(expectedResponse));
        verify(presenterValidationService).validatePresenterDetails(testValidationRequest);
        verify(responseEntityFactory).createResponse(argThat((ServiceResult<ValidationResponse> result) ->
            result.getStatus() == ServiceResultStatus.FOUND
                && result.getData().getResult() == ValidationResponse.ResultEnum.VALID));
    }

    @Test
    @DisplayName("should return an invalid response when the statement is not found")
    void shouldReturnInvalidResponseWhenStatementIsNotFound() {
        assertInvalidMockScenario("statementNotFound", "Statement not valid statement");
    }

    @Test
    @DisplayName("should return an invalid response when input validation fails")
    void shouldReturnInvalidResponseWhenInputValidationFails() {
        assertInvalidMockScenario("presenterValidation", "Input validation fails");
    }

    @Test
    @DisplayName("should return an invalid response when the presenter is not exempt")
    void shouldReturnInvalidResponseWhenPresenterIsNotExempt() {
        assertInvalidMockScenario("presenterNotExempt",
            "Presenter is not on the exemption list but the exempt statement has been given.");
    }

    private void assertInvalidMockScenario(final String statementId, final String errorMessage) {
        final var request = new ValidationRequest();
        request.setStatementIds(List.of(statementId));
        final var expectedError = Err.invalidBodyBuilderWithLocation("statement_ids")
            .withError(errorMessage)
            .build();
        final ResponseEntity<ChResponseBody<ValidationResponse>> expectedResponse =
            ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();

        when(responseEntityFactory.<ValidationResponse>createResponse(any())).thenReturn(expectedResponse);

        final var actualResponse = controller.validatePresenterDetails(request);

        assertThat(actualResponse, is(expectedResponse));
        verifyNoInteractions(presenterValidationService);
        verify(responseEntityFactory).createResponse(argThat((ServiceResult<ValidationResponse> result) ->
            result.getStatus() == ServiceResultStatus.VALIDATION_ERROR
                && result.getErrors().size() == 1
                && result.getErrors().containsError(expectedError)));
    }
}

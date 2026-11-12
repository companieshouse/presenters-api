package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.config.ResponseEntityFactoryConfig;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotExemptException;
import uk.gov.companieshouse.presentersapi.exception.PresenterValidationException;
import uk.gov.companieshouse.presentersapi.exception.StatementNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterValidationControllerImpl tests")
class PresenterValidationControllerImplTest {

    @Mock
    private PresenterValidationService presenterValidationService;

    private PresenterValidationControllerImpl controller;

    private ValidationRequest testValidationRequest;
    private ValidationResponse testValidationResponse;

    private final PluggableResponseEntityFactory responseEntityFactory =
        new ResponseEntityFactoryConfig().createResponseFactory();

    @BeforeEach
    void setUp() {
        testValidationRequest = new ValidationRequest();
        testValidationRequest.setStatementIds(List.of("statement-ok"));
        testValidationResponse = new ValidationResponse();
        testValidationResponse.setResult(ValidationResponse.ResultEnum.VALID);
        controller = new PresenterValidationControllerImpl(presenterValidationService, responseEntityFactory);
    }

    @Test
    @DisplayName("should return validation response with HTTP  200")
    void testValidatePresenterDetailsSuccess() {
        when(presenterValidationService.validatePresenterDetails(testValidationRequest))
            .thenReturn(testValidationResponse);

        final var response =
            controller.validatePresenterDetails(testValidationRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ValidationResponse.ResultEnum.VALID, response.getBody().getSuccessBody().getResult());

        verify(presenterValidationService, times(1))
            .validatePresenterDetails(testValidationRequest);
        verifyNoMoreInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should throw StatementNotFoundException when mock statement id is present")
    void testValidatePresenterDetailsStatementNotFound() {
        final var request = new ValidationRequest();
        request.setStatementIds(List.of("statementNotFound"));

        final var exception = assertThrows(
            StatementNotFoundException.class,
            () -> controller.validatePresenterDetails(request));

        assertEquals("Statement not valid statement", exception.getMessage());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should throw PresenterValidationException when mock input validation fails")
    void testValidatePresenterDetails() {
        final var request = new ValidationRequest();
        request.setStatementIds(List.of("presenterValidation"));

        final var exception = assertThrows(
            PresenterValidationException.class,
            () -> controller.validatePresenterDetails(request));

        assertEquals("Input validation fails", exception.getMessage());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should throw PresenterNotExemptException when mock details not exempt")
    void testValidatePresenterDetailsNotExempt() {
        final var request = new ValidationRequest();
        request.setStatementIds(List.of("presenterNotExempt"));

        final var exception = assertThrows(
            PresenterNotExemptException.class,
            () -> controller.validatePresenterDetails(request));

        assertEquals("Presenter is not on the exemption list but the exempt statement has been given.", exception.getMessage());
        verifyNoInteractions(presenterValidationService);
    }
}
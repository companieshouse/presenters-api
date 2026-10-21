package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotExemptException;
import uk.gov.companieshouse.presentersapi.exception.PresenterValidationException;
import uk.gov.companieshouse.presentersapi.exception.StatementNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterValidationControllerImpl tests")
class PresenterValidationControllerImplTest {

    @Mock
    private PresenterValidationService presenterValidationService;

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
    @DisplayName("should return validation response with status 200")
    void testValidatePresenterDetailsSuccess() {
        when(presenterValidationService.validatePresenterDetails(testValidationRequest))
            .thenReturn(testValidationResponse);

        ResponseEntity<ValidationResponse> response = controller.validatePresenterDetails(testValidationRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ValidationResponse.ResultEnum.VALID, response.getBody().getResult());

        verify(presenterValidationService, times(1))
            .validatePresenterDetails(testValidationRequest);
        verifyNoMoreInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should throw StatementNotFoundException when mock statement id is present")
    void testValidatePresenterDetailsStatementNotFound() {
        ValidationRequest request = new ValidationRequest();
        request.setStatementIds(List.of("statementNotFound"));

        StatementNotFoundException exception = assertThrows(
            StatementNotFoundException.class,
            () -> controller.validatePresenterDetails(request));

        assertEquals("Statement not valid statement", exception.getMessage());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should throw PresenterValidationException when mock input validation fails")
    void testValidatePresenterDetails() {
        ValidationRequest request = new ValidationRequest();
        request.setStatementIds(List.of("presenterValidation"));

        PresenterValidationException exception = assertThrows(
            PresenterValidationException.class,
            () -> controller.validatePresenterDetails(request));

        assertEquals("Input validation fails", exception.getMessage());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should throw PresenterNotExemptException when mock details not exempt")
    void testValidatePresenterDetailsNotExempt() {
        ValidationRequest request = new ValidationRequest();
        request.setStatementIds(List.of("presenterNotExempt"));

        PresenterNotExemptException exception = assertThrows(
            PresenterNotExemptException.class,
            () -> controller.validatePresenterDetails(request));

        assertEquals("Presenter is not on the exemption list but the exempt statement has been given.", exception.getMessage());
        verifyNoInteractions(presenterValidationService);
    }
}
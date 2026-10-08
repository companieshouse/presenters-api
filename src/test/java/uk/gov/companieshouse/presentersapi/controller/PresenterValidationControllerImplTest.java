package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

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
        testValidationResponse = ValidationResponse.VALID;
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
        assertEquals(ValidationResponse.VALID, response.getBody());

        verify(presenterValidationService, times(1))
            .validatePresenterDetails(testValidationRequest);
        verifyNoMoreInteractions(presenterValidationService);
    }
}
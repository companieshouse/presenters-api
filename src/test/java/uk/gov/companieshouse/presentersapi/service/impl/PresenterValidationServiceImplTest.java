package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;

@DisplayName("PresenterValidationServiceImpl tests")
class PresenterValidationServiceImplTest {

    private PresenterValidationServiceImpl service;
    private ValidationRequest validationRequest;

    @BeforeEach
    void setUp() {
        service = new PresenterValidationServiceImpl();
        validationRequest = new ValidationRequest();
    }

    @Test
    @DisplayName("should return valid response")
    void testValidatePresenterDetailsReturnsValid() {
        ValidationResponse result = service.validatePresenterDetails(validationRequest);

        assertNotNull(result);
        assertEquals(ValidationResponse.VALID, result);
    }

    @Test
    @DisplayName("should handle null request gracefully")
    void testHandleNullRequest() {
        assertDoesNotThrow(() -> {
            ValidationResponse result = service.validatePresenterDetails(null);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("should handle empty validation request")
    void testHandleEmptyValidationRequest() {
        ValidationRequest emptyRequest = new ValidationRequest();
        
        assertDoesNotThrow(() -> {
            ValidationResponse result = service.validatePresenterDetails(emptyRequest);
            assertEquals(ValidationResponse.VALID, result);
        });
    }

    @Test
    @DisplayName("should not throw exception for valid request")
    void testNoExceptionForValidRequest() {
        assertDoesNotThrow(() -> {
            service.validatePresenterDetails(validationRequest);
        });
    }
}

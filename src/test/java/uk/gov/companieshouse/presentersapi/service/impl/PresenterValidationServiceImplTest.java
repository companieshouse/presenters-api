package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        validationRequest.setFormType("AD01");
    }

    @Test
    @DisplayName("should return valid response")
    void testValidatePresenterDetailsReturnsValid() {
        final var result = service.validatePresenterDetails(validationRequest);

        assertNotNull(result);
        final var data = result.getData();
        assertEquals(ValidationResponse.ResultEnum.VALID, data.getResult());
    }

    @Test
    @DisplayName("should return invalid response for AD02")
    void testValidatePresenterDetailsReturnsInvalidForAd02() {
        validationRequest.setFormType("AD02");

        final var result = service.validatePresenterDetails(validationRequest);

        assertNotNull(result);
        final var data = result.getData();
        assertEquals(ValidationResponse.ResultEnum.INVALID, data.getResult());
        assertEquals("Statement form type mismatch", data.getReason());
    }

    @Test
    @DisplayName("should return empty response for unsupported form type")
    void testValidatePresenterDetailsReturnsEmptyForUnsupportedFormType() {
        validationRequest.setFormType("AA01");

        final var result = service.validatePresenterDetails(validationRequest);

        assertNotNull(result);
        final var data = result.getData();
        assertNull(data.getResult());
        assertNull(data.getReason());
    }

    @Test
    @DisplayName("should return empty response for null form type")
    void testValidatePresenterDetailsReturnsEmptyForNullFormType() {
        validationRequest.setFormType(null);

        final var result = service.validatePresenterDetails(validationRequest);

        assertNotNull(result);
        final var data = result.getData();
        assertNull(data.getResult());
        assertNull(data.getReason());
    }
}

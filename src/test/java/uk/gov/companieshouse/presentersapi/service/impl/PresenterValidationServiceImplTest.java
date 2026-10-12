package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        ValidationResponse result = service.validatePresenterDetails(validationRequest);

        assertNotNull(result);
        assertEquals(ValidationResponse.ResultEnum.VALID, result.getResult());
    }

    @Test
    @DisplayName("should return invalid response when formType is not AD01")
    void testValidatePresenterDetailsReturnsInvalidForNonAd01() {
        validationRequest.setFormType("LLP01");

        ValidationResponse result = service.validatePresenterDetails(validationRequest);

        assertNotNull(result);
        assertEquals(ValidationResponse.ResultEnum.INVALID, result.getResult());
        assertEquals("Statement form type mismatch", result.getReason());
    }
}

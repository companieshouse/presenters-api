package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import uk.gov.companieshouse.api.presenters.model.PresenterDetails;

@DisplayName("PresenterDetailsServiceImpl tests")
class PresenterDetailsServiceImplTest {

    private final PresenterDetailsServiceImpl service = new PresenterDetailsServiceImpl();

    @Test
    @DisplayName("should return individual details")
    void shouldReturnIndividualPresenterDetails() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "individual-filing-for-self");

        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals(3, result.getStatements().size());
        assertEquals("officer-employee-statement", result.getStatements().get(0));
        assertEquals("delivery-on-behalf-of-individual", result.getStatements().get(1));
        assertEquals("verified-statement", result.getStatements().get(2));
        assertNotNull(result.getIndividual());
        assertEquals("123456", result.getIndividual().getPersonalCode());
        assertEquals("1234", result.getIndividual().getVerificationId());
    }

    @Test
    @DisplayName("should return individual details no DOB")
    void shouldReturnIndividualPresenterDetailsNoDOB() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals(3, result.getStatements().size());
        assertEquals("officer-employee-statement", result.getStatements().get(0));
        assertEquals("delivery-on-behalf-of-firm", result.getStatements().get(1));
        assertEquals("verified-statement", result.getStatements().get(2));
        assertNotNull(result.getIndividual());
        assertEquals("123456", result.getIndividual().getPersonalCode());
        assertEquals("1234", result.getIndividual().getVerificationId());
        assertEquals(1, result.getIndividual().getDateOfBirth().getDay());
        assertEquals(1, result.getIndividual().getDateOfBirth().getMonth());
        assertEquals(1980, result.getIndividual().getDateOfBirth().getYear());
    }

    @Test
    @DisplayName("should return acsp presenter details")
    void shouldReturnAcspPresenterDetails() {
        PresenterDetails result = service.getPresenterDetails("user456", "form-abc", "acsp-employee");

        assertNotNull(result);
        assertEquals("user456", result.getUserId());
        assertEquals(2, result.getStatements().size());
        assertEquals("ascp-employee-statement", result.getStatements().get(0));
        assertEquals("delivery-on-behalf-of-firm", result.getStatements().get(1));
        assertNotNull(result.getAcsp());
        assertEquals("123456-123456", result.getAcsp().getAcspCode());
        assertEquals("Mr ACSP Ltd", result.getAcsp().getAcspName());
        assertEquals("me@myacsp.co.uk", result.getAcsp().getAcspEmployeeEmail());
    }

    @Test
    @DisplayName("should return exempt presenter details")
    void shouldReturnExemptPresenterDetails() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "official-receiver");

        assertNotNull(result);
        assertEquals(2, result.getStatements().size());
        assertEquals("exempt-statement", result.getStatements().get(0));
        assertEquals("delivery-on-behalf-of-firm", result.getStatements().get(1));
        assertNotNull(result.getExempt());
        assertEquals("me@iamexempt.co.uk", result.getExempt().getEmail());
        assertEquals("Exemption LTD", result.getExempt().getOrganisationName());
        assertEquals("Mr Test Exemption", result.getExempt().getName());
    }

    @Test
    @DisplayName("should throw exception for unknown presenter type")
    void shouldThrowForUnknownPresenterType() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.getPresenterDetails("user123", "form-abc", "unknown-type"));
        assertEquals("Unexpected value: unknown-type", exception.getMessage());
    }
}
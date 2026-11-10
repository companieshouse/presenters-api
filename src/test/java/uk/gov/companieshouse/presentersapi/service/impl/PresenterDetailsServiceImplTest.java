package uk.gov.companieshouse.presentersapi.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotFoundException;
import uk.gov.companieshouse.service.ServiceResultStatus;

@DisplayName("PresenterDetailsServiceImpl tests")
class PresenterDetailsServiceImplTest {

    private final PresenterDetailsServiceImpl service = new PresenterDetailsServiceImpl();

    @Test
    @DisplayName("should return individual details")
    void shouldReturnIndividualPresenterDetails() {
        final var result = service.getPresenterDetails("user123", "form-abc", "individual-filing-for-self");

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(notNullValue()));
        final var data = result.getData();
        assertEquals("user123", data.getUserId());
        assertEquals(1, data.getStatements().size());
        assertEquals("verified-statement", data.getStatements().getFirst());
        assertNotNull(data.getIndividual());
        assertEquals("123456", data.getIndividual().getPersonalCode());
        assertEquals("1234", data.getIndividual().getVerificationId());
        assertNull(data.getAcsp());
        assertNull(data.getExempt());
    }

    @Test
    @DisplayName("should return officer employee details with date of birth")
    void shouldReturnOfficerEmployeePresenterDetailsWithDateOfBirth() {
        final var result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(notNullValue()));
        final var data = result.getData();
        assertEquals("user123", data.getUserId());
        assertEquals(3, data.getStatements().size());
        assertEquals("officer-employee-statement", data.getStatements().get(0));
        assertEquals("verified-statement", data.getStatements().get(1));
        assertEquals("delivery-on-behalf-of-firm", data.getStatements().get(2));
        assertNotNull(data.getIndividual());
        assertEquals("123456", data.getIndividual().getPersonalCode());
        assertEquals("1234", data.getIndividual().getVerificationId());
        assertEquals(1, data.getIndividual().getDateOfBirth().getDay());
        assertEquals(1, data.getIndividual().getDateOfBirth().getMonth());
        assertEquals(1980, data.getIndividual().getDateOfBirth().getYear());
    }

    @Test
    @DisplayName("should return acsp presenter details")
    void shouldReturnAcspPresenterDetails() {
        final var result = service.getPresenterDetails("user456", "form-abc", "acsp");

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(notNullValue()));
        final var data = result.getData();
        assertEquals("user456", data.getUserId());
        assertEquals(1, data.getStatements().size());
        assertEquals("acsp-statement", data.getStatements().getFirst());
        assertNotNull(data.getAcsp());
        assertEquals("123456-123456", data.getAcsp().getAcspCode());
        assertEquals("Mr ACSP Ltd", data.getAcsp().getAcspName());
        assertEquals("me@myacsp.co.uk", data.getAcsp().getAcspEmployeeEmail());
        assertNull(data.getIndividual());
        assertNull(data.getExempt());
    }

    @Test
    @DisplayName("should return exempt presenter details")
    void shouldReturnExemptPresenterDetails() {
        final var result = service.getPresenterDetails("user123", "form-abc", "official-receiver");

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(notNullValue()));
        final var data = result.getData();
        assertEquals(2, data.getStatements().size());
        assertEquals("official-receiver-statement", data.getStatements().get(0));
        assertEquals("delivery-on-behalf-of-firm", data.getStatements().get(1));
        assertNotNull(data.getExempt());
        assertEquals("me@iamexempt.co.uk", data.getExempt().getEmail());
        assertEquals("Exemption LTD", data.getExempt().getOrganisationName());
        assertEquals("Mr Test Exemption", data.getExempt().getName());
        assertNull(data.getAcsp());
        assertNull(data.getIndividual());
    }

    @Test
    @DisplayName("should throw exception when presenter type is unsupported")
    void shouldThrowExceptionWhenPresenterTypeIsUnsupported() {
        final var exception = assertThrows(PresenterTypeNotFoundException.class,
            () -> service.getPresenterDetails("user123", "form-abc", "unsupported-type"));

        assertThat(exception.getMessage(), is("Presenter Type: unsupported-type is not found"));
    }
}
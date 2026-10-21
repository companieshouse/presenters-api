package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
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
import uk.gov.companieshouse.api.presenters.model.Exempt;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotValidAcspException;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotVerifiedException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotValidException;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterDetailsControllerImpl tests")
class PresenterDetailsControllerImplTest {

    @Mock
    private PresenterDetailsService presenterDetailsService;

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
    @DisplayName("should return presenter details with status 200")
    void testGetPresenterDetailsSuccess() {
        final var userId = "user123";
        final var formType = "form-abc";
        final var presenterType = "officer-employee";

        when(presenterDetailsService.getPresenterDetails(userId, formType, presenterType))
            .thenReturn(testPresenterDetails);

        final var response = controller.getPresenterDetails(userId, formType, presenterType);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getStatements().size());
        assertNotNull(response.getBody().getExempt());

        verify(presenterDetailsService, times(1))
            .getPresenterDetails(userId, formType, presenterType);
    }

    @Test
    @DisplayName("should pass userId, formType, and presenterType parameters to service")
    void testParametersPassedToService() {
        final var userId = "user456";
        final var formType = "form-xyz";
        final var presenterType = "corporate-officer-employee";

        when(presenterDetailsService.getPresenterDetails(userId, formType, presenterType))
            .thenReturn(testPresenterDetails);

        controller.getPresenterDetails(userId, formType, presenterType);

        verify(presenterDetailsService).getPresenterDetails(userId, formType, presenterType);
    }

    @Test
    @DisplayName("should return response entity with presenter details")
    void testResponseEntityStructure() {
        when(presenterDetailsService.getPresenterDetails(anyString(), anyString(), anyString()))
            .thenReturn(testPresenterDetails);

        final var response = controller.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getStatements());
        assertNotNull(response.getBody().getExempt());
    }

    @Test
    @DisplayName("should throw PresenterTypeNotValidException for presenterTypeNotValid")
    void shouldThrowPresenterTypeNotValidException() {
        PresenterTypeNotValidException exception = assertThrows(
            PresenterTypeNotValidException.class,
            () -> controller.getPresenterDetails("user123", "form-abc", "presenterTypeNotValid"));

        assertEquals("Presenter Type is not valid for Form Type", exception.getMessage());
        verifyNoInteractions(presenterDetailsService);
    }

    @Test
    @DisplayName("should throw PresenterNotVerifiedException for presenterTypeNotVerified")
    void shouldThrowPresenterNotVerifiedException() {
        PresenterNotVerifiedException exception = assertThrows(
            PresenterNotVerifiedException.class,
            () -> controller.getPresenterDetails("user123", "form-abc", "presenterTypeNotVerified"));

        assertEquals("User has not completed their identity verification", exception.getMessage());
        verifyNoInteractions(presenterDetailsService);
    }

    @Test
    @DisplayName("should throw PresenterNotValidAcspException for presenterNotValidAcsp")
    void shouldThrowPresenterNotValidAcspException() {
        PresenterNotValidAcspException exception = assertThrows(
            PresenterNotValidAcspException.class,
            () -> controller.getPresenterDetails("user123", "form-abc", "presenterNotValidAcsp"));

        assertEquals("User is not associated with an ACSP", exception.getMessage());
        verifyNoInteractions(presenterDetailsService);
    }

    @Test
    @DisplayName("should throw PresenterNotFoundException for presenterNotFound")
    void shouldThrowPresenterNotFoundException() {
        PresenterNotFoundException exception = assertThrows(
            PresenterNotFoundException.class,
            () -> controller.getPresenterDetails("user123", "form-abc", "presenterNotFound"));

        assertEquals("User not found", exception.getMessage());
        verifyNoInteractions(presenterDetailsService);
    }

    @Test
    @DisplayName("should throw PresenterTypeNotFoundException for presenterTypeNotFound")
    void shouldThrowPresenterTypeNotFoundException() {
        PresenterTypeNotFoundException exception = assertThrows(
            PresenterTypeNotFoundException.class,
            () -> controller.getPresenterDetails("user123", "form-abc", "presenterTypeNotFound"));

        assertEquals("presenter type not found", exception.getMessage());
        verifyNoInteractions(presenterDetailsService);
    }
}
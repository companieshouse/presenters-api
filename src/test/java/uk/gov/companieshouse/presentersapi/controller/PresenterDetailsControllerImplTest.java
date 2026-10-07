package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

import uk.gov.companieshouse.api.presenters.model.Exempt;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
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
        Exempt testExempt = new Exempt();
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
        String userId = "user123";
        String formType = "form-abc";
        String presenterType = "officer-employee";

        when(presenterDetailsService.getPresenterDetails(userId, formType, presenterType))
            .thenReturn(testPresenterDetails);

        ResponseEntity<PresenterDetails> response = controller.getPresenterDetails(userId, formType, presenterType);

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
        String userId = "user456";
        String formType = "form-xyz";
        String presenterType = "corporate-officer-employee";

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

        ResponseEntity<PresenterDetails> response = controller.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getStatements());
        assertNotNull(response.getBody().getExempt());
    }

    @Test
    @DisplayName("should verify controller delegates to service")
    void testControllerDelegation() {
        String userId = "delegation-test-user";
        String formType = "delegation-test-form";
        String presenterType = "exempt-presenter";

        when(presenterDetailsService.getPresenterDetails(userId, formType, presenterType))
            .thenReturn(testPresenterDetails);

        controller.getPresenterDetails(userId, formType, presenterType);

        verify(presenterDetailsService, times(1))
            .getPresenterDetails(userId, formType, presenterType);
        verifyNoMoreInteractions(presenterDetailsService);
    }
}
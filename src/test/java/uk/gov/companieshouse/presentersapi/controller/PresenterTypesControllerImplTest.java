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

import uk.gov.companieshouse.api.presenters.model.PresenterTypes;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypesControllerImpl tests")
class PresenterTypesControllerImplTest {

    @Mock
    private PresenterTypesService presenterTypesService;

    @InjectMocks
    private PresenterTypesControllerImpl controller;

    private PresenterTypes testPresenterTypes;

    @BeforeEach
    void setUp() {
        testPresenterTypes = new PresenterTypes();
        testPresenterTypes.setTypes(List.of(
            "officer-employee",
            "corporate-officer-employee",
            "exempt-presenter",
            "acsp-sole-trader",
            "acsp-employee",
            "individual-filing-for-self",
            "individual-filing-for-someone-else"
        ));
    }

    @Test
    @DisplayName("should return presenter types with status 200")
    void testGetPresenterTypesSuccess() {
        String userId = "user123";
        String formType = "form-abc";

        when(presenterTypesService.getPresenterTypes(userId, formType))
            .thenReturn(testPresenterTypes);

        ResponseEntity<PresenterTypes> response = controller.getPresenterTypes(userId, formType);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(7, response.getBody().getTypes().size());
        assertEquals("officer-employee", response.getBody().getTypes().getFirst());

        verify(presenterTypesService, times(1))
            .getPresenterTypes(userId, formType);
    }

    @Test
    @DisplayName("should pass userId and formType parameters to service")
    void testParametersPassedToService() {
        String userId = "user456";
        String formType = "form-xyz";

        when(presenterTypesService.getPresenterTypes(userId, formType))
            .thenReturn(testPresenterTypes);

        controller.getPresenterTypes(userId, formType);

        verify(presenterTypesService).getPresenterTypes(userId, formType);
    }

    @Test
    @DisplayName("should return empty list when service returns empty types")
    void testEmptyPresenterTypes() {
        PresenterTypes emptyTypes = new PresenterTypes();
        emptyTypes.setTypes(List.of());

        when(presenterTypesService.getPresenterTypes(anyString(), anyString()))
            .thenReturn(emptyTypes);

        ResponseEntity<PresenterTypes> response = controller.getPresenterTypes("user789", "form-empty");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assert response.getBody() != null;
        assertEquals(0, response.getBody().getTypes().size());
    }

    @Test
    @DisplayName("should return response entity with presenter types")
    void testResponseEntityStructure() {
        when(presenterTypesService.getPresenterTypes(anyString(), anyString()))
            .thenReturn(testPresenterTypes);

        ResponseEntity<PresenterTypes> response = controller.getPresenterTypes("user123", "form-abc");

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTypes());
        assertFalse(response.getBody().getTypes().isEmpty());
    }

    @Test
    @DisplayName("should verify controller delegates to service")
    void testControllerDelegation() {
        String userId = "delegation-test-user";
        String formType = "delegation-test-form";

        when(presenterTypesService.getPresenterTypes(userId, formType))
            .thenReturn(testPresenterTypes);

        controller.getPresenterTypes(userId, formType);

        verify(presenterTypesService, times(1))
            .getPresenterTypes(userId, formType);
        verifyNoMoreInteractions(presenterTypesService);
    }

    @Test
    @DisplayName("should return types containing all expected presenter types")
    void testAllExpectedTypes() {
        when(presenterTypesService.getPresenterTypes(anyString(), anyString()))
            .thenReturn(testPresenterTypes);

        ResponseEntity<PresenterTypes> response = controller.getPresenterTypes("user", "form");

        assert response.getBody() != null;
        List<String> types = response.getBody().getTypes();
        assertTrue(types.contains("officer-employee"));
        assertTrue(types.contains("corporate-officer-employee"));
        assertTrue(types.contains("exempt-presenter"));
        assertTrue(types.contains("acsp-sole-trader"));
        assertTrue(types.contains("acsp-employee"));
        assertTrue(types.contains("individual-filing-for-self"));
        assertTrue(types.contains("individual-filing-for-someone-else"));
    }
}
package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.companieshouse.api.presenters.model.PresenterTypes;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.InternalServerError;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypesControllerImpl tests")
class PresenterTypesControllerImplTest {

    @Mock
    private PresenterTypesService presenterTypesService;

    @InjectMocks
    private PresenterTypesControllerImpl controller;

    @Test
    @DisplayName("should throw FormTypeNotFoundException when formType is formTypeNotFound")
    void shouldThrowFormTypeNotFoundException() {
        FormTypeNotFoundException exception = assertThrows(
            FormTypeNotFoundException.class,
            () -> controller.getPresenterTypes("user123", "formTypeNotFound"));

        assertEquals("Form Type: formTypeNotFound", exception.getMessage());
        verifyNoInteractions(presenterTypesService);
    }

    @Test
    @DisplayName("should throw PresenterNotFoundException when userId is presenterNotFound")
    void shouldThrowPresenterNotFoundException() {
        PresenterNotFoundException exception = assertThrows(
            PresenterNotFoundException.class,
            () -> controller.getPresenterTypes("presenterNotFound", "AD01"));

        assertEquals("Presenter not found with UserId: presenterNotFound", exception.getMessage());
        verifyNoInteractions(presenterTypesService);
    }

    @Test
    @DisplayName("should throw InternalServerError when userId is internalServerError")
    void shouldThrowInternalServerError() {
        InternalServerError exception = assertThrows(
            InternalServerError.class,
            () -> controller.getPresenterTypes("internalServerError", "AD01"));

        assertEquals("An unexpected error occurred while processing the request.", exception.getMessage());
        verifyNoInteractions(presenterTypesService);
    }

    @Test
    @DisplayName("should return presenter types when applyMockScenarios has no match")
    void shouldReturnPresenterTypesWhenNoMockScenarioMatches() {
        EnumSet<PresenterType> serviceResponse = EnumSet.allOf(PresenterType.class);
        when(presenterTypesService.getPresenterTypes("user123", "AD01")).thenReturn(serviceResponse);

        ResponseEntity<PresenterTypes> response = controller.getPresenterTypes("user123", "AD01");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        List<String> expectedTypes = serviceResponse.stream().map(PresenterType::code).toList();
        assertEquals(expectedTypes, response.getBody().getTypes());
        verify(presenterTypesService).getPresenterTypes("user123", "AD01");
    }
}
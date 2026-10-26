package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import uk.gov.companieshouse.api.presenters.model.PresenterTypes;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.InternalServerError;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypesControllerImpl tests")
class PresenterTypesControllerImplTest {

    private static final String ERIC_ACCESS_TOKEN_HEADER = "ERIC-Access-Token";
    private static final String PASSTHROUGH_TOKEN = "passthrough-token";

    @Mock
    private PresenterTypesService presenterTypesService;

    private MockHttpServletRequest request;

    @InjectMocks
    private PresenterTypesControllerImpl controller;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.addHeader(ERIC_ACCESS_TOKEN_HEADER, PASSTHROUGH_TOKEN);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

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
        when(presenterTypesService.getPresenterTypes("user123", "AD01", PASSTHROUGH_TOKEN))
            .thenReturn(serviceResponse);

        ResponseEntity<PresenterTypes> response = controller.getPresenterTypes("user123", "AD01");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        List<String> expectedTypes = serviceResponse.stream().map(PresenterType::code).toList();
        assertEquals(expectedTypes, response.getBody().getTypes());
        verify(presenterTypesService).getPresenterTypes("user123", "AD01", PASSTHROUGH_TOKEN);
    }

    @Test
    @DisplayName("should reject request with 400 when the ERIC passthrough token is missing")
    void shouldRejectRequestWhenPassthroughTokenMissing() {
        request.removeHeader(ERIC_ACCESS_TOKEN_HEADER);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.getPresenterTypes("user123", "AD01"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(presenterTypesService);
    }
}
package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.presentersapi.config.ResponseEntityFactoryConfig;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypesControllerImpl tests")
class PresenterTypesControllerImplTest {

    private static final String ERIC_ACCESS_TOKEN_HEADER = "ERIC-Access-Token";
    private static final String PASSTHROUGH_TOKEN = "passthrough-token";
    private static final String DEFAULT_USER_ID = "user123";

    @Mock
    private PresenterTypesService presenterTypesService;

    private MockHttpServletRequest request;

    private PresenterTypesControllerImpl controller;

    private final PluggableResponseEntityFactory responseEntityFactory =
        new ResponseEntityFactoryConfig().createResponseFactory();

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.addHeader(ERIC_ACCESS_TOKEN_HEADER, PASSTHROUGH_TOKEN);
        setOauth2User(DEFAULT_USER_ID);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        controller = new PresenterTypesControllerImpl(presenterTypesService, responseEntityFactory);
    }

    private void setOauth2User(final String userId) {
        request.removeHeader("ERIC-Identity");
        request.addHeader("ERIC-Identity", userId);
        request.removeHeader("ERIC-Identity-Type");
        request.addHeader("ERIC-Identity-Type", "oauth2");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("should throw FormTypeNotFoundException when formType is formTypeNotFound")
    void shouldThrowFormTypeNotFoundException() {
        final var exception = assertThrows(
            FormTypeNotFoundException.class,
            () -> controller.getPresenterTypes("formTypeNotFound"));

        assertEquals("Form Type: formTypeNotFound", exception.getMessage());
        verifyNoInteractions(presenterTypesService);
    }

    @Test
    @DisplayName("should return presenter types when applyMockScenarios has no match")
    void shouldReturnPresenterTypesWhenNoMockScenarioMatches() {
        final var serviceResponse = EnumSet.allOf(PresenterType.class);
        when(presenterTypesService.getPresenterTypes("user123", "AD01", PASSTHROUGH_TOKEN))
            .thenReturn(serviceResponse);

        final var response = controller.getPresenterTypes("AD01");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final var expectedTypes = serviceResponse.stream().map(PresenterType::code).toList();
        assertEquals(expectedTypes, response.getBody().getSuccessBody().getTypes());
        verify(presenterTypesService).getPresenterTypes("user123", "AD01", PASSTHROUGH_TOKEN);
    }

    @Test
    @DisplayName("should reject request with HTTP 400 when the ERIC passthrough token is missing")
    void shouldRejectRequestWhenPassthroughTokenMissing() {
        request.removeHeader(ERIC_ACCESS_TOKEN_HEADER);

        final var exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.getPresenterTypes("AD01"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(presenterTypesService);
    }
}

package uk.gov.companieshouse.presentersapi.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypesControllerImpl tests")
class PresenterTypesControllerImplTest {

    private static final String ERIC_ACCESS_TOKEN_HEADER = "ERIC-Access-Token";
    private static final String PASSTHROUGH_TOKEN = "passthrough-token";

    @Mock
    private PresenterTypesService presenterTypesService;

    @Mock
    private PluggableResponseEntityFactory responseEntityFactory;

    @InjectMocks
    private PresenterTypesControllerImpl controller;

    private MockHttpServletRequest request;

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
    @DisplayName("should return presenter types when the service finds them")
    void shouldReturnPresenterTypesWhenServiceFindsThem() {
        final var formType = "AD01";
        final var presenterTypes = EnumSet.allOf(PresenterType.class);
        final ResponseEntity<ChResponseBody<PresenterTypes>> expectedResponse = ResponseEntity.ok().build();

        when(presenterTypesService.getPresenterTypes(null, formType, PASSTHROUGH_TOKEN))
            .thenReturn(ServiceResult.found(presenterTypes));
        when(responseEntityFactory.<PresenterTypes>createResponse(any())).thenReturn(expectedResponse);

        final var actualResponse = controller.getPresenterTypes(formType);

        assertThat(actualResponse, is(expectedResponse));
        verify(presenterTypesService).getPresenterTypes(null, formType, PASSTHROUGH_TOKEN);
        verify(responseEntityFactory).createResponse(argThat((ServiceResult<PresenterTypes> result) ->
            result.getStatus() == ServiceResultStatus.FOUND
                && result.getData().getTypes().equals(presenterTypes.stream().map(PresenterType::code).toList())));
    }

    @Test
    @DisplayName("should return an invalid response when the service rejects the request")
    void shouldReturnInvalidResponseWhenServiceRejectsRequest() {
        final var formType = "unsupported";
        final var errors = new Errors(Err.invalidBodyBuilderWithLocation("form_type")
            .withError("Form Type not supported")
            .addErrorValue("form_type", formType)
            .build());
        final ResponseEntity<ChResponseBody<PresenterTypes>> expectedResponse = ResponseEntity.badRequest().build();

        when(presenterTypesService.getPresenterTypes(null, formType, PASSTHROUGH_TOKEN))
            .thenReturn(ServiceResult.invalid(errors));
        when(responseEntityFactory.<PresenterTypes>createResponse(any())).thenReturn(expectedResponse);

        final var actualResponse = controller.getPresenterTypes(formType);

        assertThat(actualResponse, is(expectedResponse));
        verify(presenterTypesService).getPresenterTypes(null, formType, PASSTHROUGH_TOKEN);
        verify(responseEntityFactory).createResponse(argThat((ServiceResult<PresenterTypes> result) ->
            result.getErrors().equals(errors)));
    }

    @Test
    @DisplayName("should reject the request with 400 when the ERIC passthrough token is missing")
    void shouldRejectRequestWhenPassthroughTokenMissing() {
        request.removeHeader(ERIC_ACCESS_TOKEN_HEADER);

        final var exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.getPresenterTypes("AD01"));

        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(presenterTypesService, responseEntityFactory);
    }
}

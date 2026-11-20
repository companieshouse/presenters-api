package uk.gov.companieshouse.presentersapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.config.ResponseEntityFactoryConfig;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.FormGroup;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.guard.FormTypeGuard;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterValidationControllerImpl tests")
class PresenterValidationControllerImplTest {

    @Mock
    private PresenterValidationService presenterValidationService;

    private PresenterValidationControllerImpl controller;
    private FormTypeGuard formTypeGuard;

    private ValidationRequest testValidationRequest;
    private ValidationResponse testValidationResponse;

    private final PluggableResponseEntityFactory responseEntityFactory =
        new ResponseEntityFactoryConfig().createResponseFactory();

    @BeforeEach
    void setUp() {
        testValidationRequest = new ValidationRequest();
        testValidationRequest.setFormType("AD01");
        testValidationRequest.setStatementIds(List.of(AssociationStatementType.ACSP_STATEMENT.code()));
        testValidationRequest.setDataTypes(List.of(DataObjectType.PERSONAL_CODE.code()));
        testValidationResponse = new ValidationResponse();
        testValidationResponse.setResult(ValidationResponse.ResultEnum.VALID);
        final var formTypesByGroup = Map.of(FormGroup.EXEMPT_FORM, Set.of(FormType.LIQ04));
        formTypeGuard = new FormTypeGuard(new FormGroupFormTypeBiMap(formTypesByGroup));
        controller = new PresenterValidationControllerImpl(
            presenterValidationService, responseEntityFactory, formTypeGuard);
    }

    @Test
    @DisplayName("should return validation response with HTTP 200")
    void shouldReturnValidationResponseWhenRequestIsValid() {
        when(presenterValidationService.validatePresenterDetails(testValidationRequest))
            .thenReturn(ServiceResult.found(testValidationResponse));

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ValidationResponse.ResultEnum.VALID, response.getBody().getSuccessBody().getResult());

        verify(presenterValidationService).validatePresenterDetails(testValidationRequest);
        verifyNoMoreInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should return HTTP 422 when presenter validation is unprocessable")
    void shouldReturnUnprocessableWhenPresenterValidationFails() {
        final var errors = new Errors(Err.serviceErrBuilder().withError("Statement mismatch").build());
        when(presenterValidationService.validatePresenterDetails(testValidationRequest))
            .thenReturn(ServiceResult.unprocessable(errors));

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(presenterValidationService).validatePresenterDetails(testValidationRequest);
        verifyNoMoreInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should return HTTP 400 when a statement id is invalid")
    void shouldReturnBadRequestWhenStatementIdIsInvalid() {
        testValidationRequest.setStatementIds(List.of("statementNotFound"));

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should return HTTP 400 when statement ids are duplicated")
    void shouldReturnBadRequestWhenStatementIdsAreDuplicated() {
        testValidationRequest.setStatementIds(List.of(
            AssociationStatementType.ACSP_STATEMENT.code(),
            AssociationStatementType.ACSP_STATEMENT.code()));

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should return HTTP 400 when a data type is invalid")
    void shouldReturnBadRequestWhenDataTypeIsInvalid() {
        testValidationRequest.setDataTypes(List.of("unknown-data-type"));

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should return HTTP 400 when data types are duplicated")
    void shouldReturnBadRequestWhenDataTypesAreDuplicated() {
        testValidationRequest.setDataTypes(List.of(
            DataObjectType.PERSONAL_CODE.code(),
            DataObjectType.PERSONAL_CODE.code()));

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should return HTTP 400 when form type is invalid")
    void shouldReturnBadRequestWhenFormTypeIsInvalid() {
        testValidationRequest.setFormType("unknown-form");

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should return HTTP 400 when form type is in the exempt-form group")
    void shouldReturnBadRequestWhenFormTypeIsInExemptFormGroup() {
        testValidationRequest.setFormType("LIQ04");

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verifyNoInteractions(presenterValidationService);
    }

    @Test
    @DisplayName("should report form type, statement id and data type violations together")
    void shouldReportAllRequestViolationsTogether() {
        testValidationRequest.setFormType("unknown-form");
        testValidationRequest.setStatementIds(List.of(
            AssociationStatementType.ACSP_STATEMENT.code(),
            AssociationStatementType.ACSP_STATEMENT.code()));
        testValidationRequest.setDataTypes(List.of(
            DataObjectType.PERSONAL_CODE.code(),
            DataObjectType.PERSONAL_CODE.code()));

        final var response = controller.validatePresenterDetails(testValidationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getErrorBody().size());
        verifyNoInteractions(presenterValidationService);
    }
}
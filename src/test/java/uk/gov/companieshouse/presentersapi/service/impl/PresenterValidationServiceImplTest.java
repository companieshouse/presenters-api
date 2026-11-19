package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;
import uk.gov.companieshouse.presentersapi.service.DataObjectTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.StatementTypeMatrixService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;

@ExtendWith(MockitoExtension.class)
class PresenterValidationServiceImplTest {

    @Mock
    private PresenterTypeMatrixService presenterTypeMatrixService;
    @Mock
    private DataObjectTypeMatrixService dataObjectTypeMatrixService;
    @Mock
    private StatementTypeMatrixService statementTypeMatrixService;

    private PresenterValidationServiceImpl service;
    private ValidationRequest validationRequest;

    @BeforeEach
    void setUp() {
        service = new PresenterValidationServiceImpl(
            presenterTypeMatrixService, dataObjectTypeMatrixService, statementTypeMatrixService);
        validationRequest = new ValidationRequest();
        validationRequest.setFormType("AD01");
        validationRequest.setDataTypes(List.of("personal-code", "name", "date-of-birth"));
        validationRequest.setStatementIds(List.of(
            "officer-employee-statement", "delivery-on-behalf-of-firm", "verified-statement"));
    }

    @Test
    void shouldReturnValidWhenOnePresenterMatchesDataAndStatementTypes() {
        givenPresenters(PresenterType.OFFICER_EMPLOYEE);
        givenDataTypes(PresenterType.OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        givenStatementCombinations(PresenterType.OFFICER_EMPLOYEE, Set.of(officerEmployeeStatements(
            DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM)));

        final var result = service.validatePresenterDetails(validationRequest);

        assertEquals(ServiceResultStatus.FOUND, result.getStatus());
        assertNotNull(result.getData());
        assertEquals(ValidationResponse.ResultEnum.VALID, result.getData().getResult());
    }

    @Test
    void shouldRequireTheSamePresenterToMatchBothDataAndStatements() {
        givenPresenters(PresenterType.OFFICER_EMPLOYEE, PresenterType.CORPORATE_OFFICER_EMPLOYEE);
        givenDataTypes(PresenterType.OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        givenDataTypes(PresenterType.CORPORATE_OFFICER_EMPLOYEE,
            DataObjectType.ACSP_CODE, DataObjectType.ACSP_NAME, DataObjectType.EMAIL, DataObjectType.NAME);
        givenStatementCombinations(PresenterType.OFFICER_EMPLOYEE,
            Set.of(statement(AssociationStatementType.JUDICIAL_FACTOR_STATEMENT)));

        final var result = service.validatePresenterDetails(validationRequest);

        assertUnprocessableReason(result, "statement_ids", "Statements do not match for form AD01.");
        verify(statementTypeMatrixService, atLeastOnce())
            .validStatementCombinationsFor(PresenterType.OFFICER_EMPLOYEE, FormType.AD01);
        verify(statementTypeMatrixService, never())
            .validStatementCombinationsFor(PresenterType.CORPORATE_OFFICER_EMPLOYEE, FormType.AD01);
    }

    @Test
    void shouldReturnInvalidWhenNoPresenterIsPermittedForForm() {
        givenPresenters();

        final var result = service.validatePresenterDetails(validationRequest);

        assertEquals(ServiceResultStatus.UNPROCESSABLE, result.getStatus());
        assertTrue(result.getErrors().containsError(Err.serviceErrBuilder()
            .withError("No candidate presenter type supports the supplied form type: AD01").build()));
        verifyNoInteractions(dataObjectTypeMatrixService, statementTypeMatrixService);
    }

    @Test
    void shouldReportDataTypeMismatch() {
        givenPresenters(PresenterType.OFFICER_EMPLOYEE);
        validationRequest.setDataTypes(List.of("personal-code", "name"));
        givenDataTypes(PresenterType.OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);

        final var result = service.validatePresenterDetails(validationRequest);

        assertUnprocessableReason(result, "data_types", "Data types do not match for form AD01.");
        verifyNoInteractions(statementTypeMatrixService);
    }

    @Test
    void shouldReportDataTypeMismatchWhenCandidatesTie() {
        givenPresenters(PresenterType.OFFICER_EMPLOYEE, PresenterType.CORPORATE_OFFICER_EMPLOYEE);
        validationRequest.setDataTypes(List.of("personal-code", "name", "date-of-birth", "email"));
        givenDataTypes(PresenterType.OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        givenDataTypes(PresenterType.CORPORATE_OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);

        final var result = service.validatePresenterDetails(validationRequest);

        assertUnprocessableReason(result, "data_types", "Data types do not match for form AD01.");
        verifyNoInteractions(statementTypeMatrixService);
    }

    @Test
    void shouldReportDataTypeMismatchWhenThereAreFurtherCandidates() {
        givenPresenters(PresenterType.OFFICER_EMPLOYEE, PresenterType.CORPORATE_OFFICER_EMPLOYEE,
            PresenterType.JUDICIAL_FACTOR);
        validationRequest.setDataTypes(List.of("personal-code", "name", "date-of-birth", "email"));
        givenDataTypes(PresenterType.OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        givenDataTypes(PresenterType.CORPORATE_OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        givenDataTypes(PresenterType.JUDICIAL_FACTOR, DataObjectType.NAME);

        final var result = service.validatePresenterDetails(validationRequest);

        assertUnprocessableReason(result, "data_types", "Data types do not match for form AD01.");
        verifyNoInteractions(statementTypeMatrixService);
    }

    @Test
    void shouldReportStatementMismatchForSingleCandidate() {
        givenPresenters(PresenterType.OFFICER_EMPLOYEE);
        givenDataTypes(PresenterType.OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        validationRequest.setStatementIds(List.of(
            "officer-employee-statement", "delivery-on-behalf-of-individual", "verified-statement"));
        givenStatementCombinations(PresenterType.OFFICER_EMPLOYEE, Set.of(officerEmployeeStatements(
            DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM)));

        final var result = service.validatePresenterDetails(validationRequest);

        assertUnprocessableReason(result, "statement_ids", "Statements do not match for form AD01.");
    }

    @Test
    void shouldReportStatementMismatchForAcspDeliveryAlternatives() {
        validationRequest.setFormType("MR01");
        givenPresenters(PresenterType.ACSP);
        validationRequest.setDataTypes(List.of("acsp-code", "acsp-name", "email", "name"));
        validationRequest.setStatementIds(List.of("acsp-statement"));
        givenDataTypes(PresenterType.ACSP,
            DataObjectType.ACSP_CODE, DataObjectType.ACSP_NAME, DataObjectType.EMAIL, DataObjectType.NAME);
        givenStatementCombinations(PresenterType.ACSP, Set.of(
            acspStatements(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM),
            acspStatements(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL)));

        final var result = service.validatePresenterDetails(validationRequest);

        assertUnprocessableReason(result, "statement_ids", "Statements do not match for form MR01.");
    }

    @Test
    void shouldReportStatementMismatchWhenCandidatesTie() {
        givenPresenters(PresenterType.OFFICER_EMPLOYEE, PresenterType.CORPORATE_OFFICER_EMPLOYEE,
            PresenterType.JUDICIAL_FACTOR);
        givenDataTypes(PresenterType.OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        givenDataTypes(PresenterType.CORPORATE_OFFICER_EMPLOYEE,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        givenDataTypes(PresenterType.JUDICIAL_FACTOR,
            DataObjectType.PERSONAL_CODE, DataObjectType.NAME, DataObjectType.DATE_OF_BIRTH);
        validationRequest.setStatementIds(List.of("officer-employee-statement", "acsp-statement"));
        givenStatementCombinations(PresenterType.OFFICER_EMPLOYEE, Set.of(statement(
            AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT)));
        givenStatementCombinations(PresenterType.CORPORATE_OFFICER_EMPLOYEE, Set.of(statement(
            AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT)));
        givenStatementCombinations(PresenterType.JUDICIAL_FACTOR, Set.of(statement(
            AssociationStatementType.JUDICIAL_FACTOR_STATEMENT)));

        final var result = service.validatePresenterDetails(validationRequest);

        assertUnprocessableReason(result, "statement_ids", "Statements do not match for form AD01.");
    }

    @Test
    void shouldAcceptExactlyOneAcspDeliveryAlternative() {
        validationRequest.setFormType("MR01");
        givenPresenters(PresenterType.ACSP);
        validationRequest.setDataTypes(List.of("acsp-code", "acsp-name", "email", "name"));
        validationRequest.setStatementIds(List.of("acsp-statement", "delivery-on-behalf-of-individual"));
        givenDataTypes(PresenterType.ACSP,
            DataObjectType.ACSP_CODE, DataObjectType.ACSP_NAME, DataObjectType.EMAIL, DataObjectType.NAME);
        givenStatementCombinations(PresenterType.ACSP, Set.of(
            acspStatements(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM),
            acspStatements(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL)));

        final var result = service.validatePresenterDetails(validationRequest);

        assertEquals(ValidationResponse.ResultEnum.VALID, result.getData().getResult());
    }

    private void givenPresenters(final PresenterType... presenterTypes) {
        final var presenters = EnumSet.noneOf(PresenterType.class);
        presenters.addAll(List.of(presenterTypes));
        final var formType = FormType.find(validationRequest.getFormType()).orElseThrow();
        when(presenterTypeMatrixService.presenterTypesAllowedFor(formType)).thenReturn(presenters);
    }

    private void givenDataTypes(final PresenterType presenterType, final DataObjectType... dataTypes) {
        final var types = EnumSet.noneOf(DataObjectType.class);
        types.addAll(List.of(dataTypes));
        when(dataObjectTypeMatrixService.dataObjectTypesAllowedFor(presenterType)).thenReturn(types);
    }

    private void givenStatementCombinations(
        final PresenterType presenterType, final Set<PresenterStatements> combinations) {
        final var formType = FormType.find(validationRequest.getFormType()).orElseThrow();
        when(statementTypeMatrixService.validStatementCombinationsFor(presenterType, formType))
            .thenReturn(combinations);
    }

    private static PresenterStatements officerEmployeeStatements(final DeliveryStatementType deliveryType) {
        return new PresenterStatements(
            EnumSet.of(AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT),
            EnumSet.of(deliveryType),
            EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT));
    }

    private static PresenterStatements acspStatements(final DeliveryStatementType deliveryType) {
        return new PresenterStatements(
            EnumSet.of(AssociationStatementType.ACSP_STATEMENT),
            EnumSet.of(deliveryType),
            EnumSet.noneOf(VerificationStatementType.class));
    }

    private static PresenterStatements statement(final AssociationStatementType statementType) {
        return new PresenterStatements(
            EnumSet.of(statementType),
            EnumSet.noneOf(DeliveryStatementType.class),
            EnumSet.noneOf(VerificationStatementType.class));
    }

    private static void assertUnprocessableReason(
        final ServiceResult<ValidationResponse> result, final String location, final String reason) {
        assertEquals(ServiceResultStatus.UNPROCESSABLE, result.getStatus());
        final var expectedError = Err.invalidBodyBuilderWithLocation(location).withError(reason).build();
        assertTrue(result.getErrors().containsError(expectedError));
    }
}

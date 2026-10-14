package uk.gov.companieshouse.presentersapi.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupDeliveryStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeAssociationStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeDeliveryStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeVerificationStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.FormGroup;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;

@ExtendWith(MockitoExtension.class)
class StatementTypeMatrixServiceImplTest {

    @Mock
    private PresenterTypeAssociationStatementTypeBiMap associationStatementBiMap;
    @Mock
    private PresenterTypeDeliveryStatementTypeBiMap deliveryStatementBiMap;
    @Mock
    private PresenterTypeVerificationStatementTypeBiMap verificationStatementBiMap;
    @Mock
    private FormGroupFormTypeBiMap formTypeBiMap;
    @Mock
    private FormGroupDeliveryStatementTypeBiMap formGroupDeliveryStatementBiMap;

    private StatementTypeMatrixServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StatementTypeMatrixServiceImpl(associationStatementBiMap, deliveryStatementBiMap,
            verificationStatementBiMap, formTypeBiMap, formGroupDeliveryStatementBiMap);
    }

    @Test
    void shouldResolveStatementsForOfficerEmployee() {
        when(associationStatementBiMap.associationStatementTypesFor(PresenterType.OFFICER_EMPLOYEE))
            .thenReturn(EnumSet.of(AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT));
        when(verificationStatementBiMap.verificationStatementTypesFor(PresenterType.OFFICER_EMPLOYEE))
            .thenReturn(EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT));
        when(deliveryStatementBiMap.deliveryStatementTypesFor(PresenterType.OFFICER_EMPLOYEE))
            .thenReturn(EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM));
        stubFormGroup(FormType.AD01, FormGroup.FIRM_DELIVERY, DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM);

        final var statements = service.statementTypesFor(PresenterType.OFFICER_EMPLOYEE, FormType.AD01);

        assertThat(statements.associationStatements(), contains(AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT));
        assertThat(statements.deliveryStatements(), contains(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM));
        assertThat(statements.verificationStatements(), contains(VerificationStatementType.VERIFIED_STATEMENT));
    }

    @ParameterizedTest
    @MethodSource("singleDeliveryFormGroups")
    void shouldResolveAcspDeliveryStatementFromFormGroup(
        final FormType formType, final FormGroup formGroup, final DeliveryStatementType deliveryStatement) {
        when(associationStatementBiMap.associationStatementTypesFor(PresenterType.ACSP))
            .thenReturn(EnumSet.of(AssociationStatementType.ACSP_STATEMENT));
        stubEmptyDeliveryAndVerification(PresenterType.ACSP);
        stubFormGroup(formType, formGroup, deliveryStatement);

        final var statements = service.statementTypesFor(PresenterType.ACSP, formType);

        assertThat(statements.associationStatements(), contains(AssociationStatementType.ACSP_STATEMENT));
        assertThat(statements.deliveryStatements(), contains(deliveryStatement));
        assertThat(statements.verificationStatements(), empty());
        verify(formGroupDeliveryStatementBiMap).deliveryStatementTypesFor(formGroup);
    }

    @ParameterizedTest
    @MethodSource("singleDeliveryFormGroups")
    void shouldResolveOfficialReceiverDeliveryStatementFromFormGroup(
        final FormType formType, final FormGroup formGroup, final DeliveryStatementType deliveryStatement) {
        when(associationStatementBiMap.associationStatementTypesFor(PresenterType.OFFICIAL_RECEIVER))
            .thenReturn(EnumSet.of(AssociationStatementType.OFFICIAL_RECEIVER_STATEMENT));
        stubEmptyDeliveryAndVerification(PresenterType.OFFICIAL_RECEIVER);
        stubFormGroup(formType, formGroup, deliveryStatement);

        final var statements = service.statementTypesFor(PresenterType.OFFICIAL_RECEIVER, formType);

        assertThat(statements.associationStatements(), contains(AssociationStatementType.OFFICIAL_RECEIVER_STATEMENT));
        assertThat(statements.deliveryStatements(), contains(deliveryStatement));
        assertThat(statements.verificationStatements(), empty());
        verify(formGroupDeliveryStatementBiMap).deliveryStatementTypesFor(formGroup);
    }

    @ParameterizedTest
    @MethodSource("multiDeliveryScenarios")
    void shouldResolveStatementsForMultiDeliveryFormGroup(
        final PresenterType presenterType,
        final AssociationStatementType associationStatement,
        final EnumSet<VerificationStatementType> verificationStatements,
        final List<String> expectedCodes) {
        when(associationStatementBiMap.associationStatementTypesFor(presenterType))
            .thenReturn(EnumSet.of(associationStatement));
        when(verificationStatementBiMap.verificationStatementTypesFor(presenterType))
            .thenReturn(verificationStatements);
        when(deliveryStatementBiMap.deliveryStatementTypesFor(presenterType))
            .thenReturn(presenterType == PresenterType.OFFICER_EMPLOYEE
                ? EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM)
                : EnumSet.noneOf(DeliveryStatementType.class));
        when(formTypeBiMap.formGroupFor(FormType.MR01)).thenReturn(FormGroup.MULTI_DELIVERY);
        when(formGroupDeliveryStatementBiMap.deliveryStatementTypesFor(FormGroup.MULTI_DELIVERY))
            .thenReturn(EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM,
                DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL));

        final var statements = service.statementTypesFor(presenterType, FormType.MR01);

        assertThat(statements.associationStatements(), contains(associationStatement));
        assertThat(statements.deliveryStatements(), containsInAnyOrder(
            DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM,
            DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL));
        assertThat(statements.verificationStatements(), is(verificationStatements));
        assertThat(statements.statementCodes(), is(expectedCodes));
    }

    @Test
    void shouldRejectNullFormType() {
        when(associationStatementBiMap.associationStatementTypesFor(PresenterType.ACSP))
            .thenReturn(EnumSet.of(AssociationStatementType.ACSP_STATEMENT));
        stubEmptyDeliveryAndVerification(PresenterType.ACSP);
        when(formTypeBiMap.formGroupFor(null)).thenThrow(new NullPointerException("value"));

        assertThrows(NullPointerException.class, () -> service.statementTypesFor(PresenterType.ACSP, null));
        verifyNoInteractions(formGroupDeliveryStatementBiMap);
    }

    @ParameterizedTest
    @EnumSource(value = FormType.class, names = {"AD01", "PSC01", "MR01"})
    void shouldSkipDeliveryLookupsForIndividualFilingForSelf(final FormType formType) {
        when(associationStatementBiMap.associationStatementTypesFor(PresenterType.INDIVIDUAL_FILING_FOR_SELF))
            .thenReturn(EnumSet.noneOf(AssociationStatementType.class));
        when(verificationStatementBiMap.verificationStatementTypesFor(PresenterType.INDIVIDUAL_FILING_FOR_SELF))
            .thenReturn(EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT));

        final var statements = service.statementTypesFor(PresenterType.INDIVIDUAL_FILING_FOR_SELF, formType);

        assertThat(statements.associationStatements(), empty());
        assertThat(statements.deliveryStatements(), empty());
        assertThat(statements.verificationStatements(), contains(VerificationStatementType.VERIFIED_STATEMENT));
        verify(associationStatementBiMap).associationStatementTypesFor(PresenterType.INDIVIDUAL_FILING_FOR_SELF);
        verifyNoInteractions(deliveryStatementBiMap, formTypeBiMap, formGroupDeliveryStatementBiMap);
    }

    private void stubEmptyDeliveryAndVerification(final PresenterType presenterType) {
        when(deliveryStatementBiMap.deliveryStatementTypesFor(presenterType))
            .thenReturn(EnumSet.noneOf(DeliveryStatementType.class));
        when(verificationStatementBiMap.verificationStatementTypesFor(presenterType))
            .thenReturn(EnumSet.noneOf(VerificationStatementType.class));
    }

    private void stubFormGroup(
        final FormType formType, final FormGroup formGroup, final DeliveryStatementType deliveryStatement) {
        when(formTypeBiMap.formGroupFor(formType)).thenReturn(formGroup);
        when(formGroupDeliveryStatementBiMap.deliveryStatementTypesFor(formGroup))
            .thenReturn(EnumSet.of(deliveryStatement));
    }

    private static Stream<Arguments> multiDeliveryScenarios() {
        return Stream.of(
            Arguments.of(PresenterType.OFFICER_EMPLOYEE, AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT,
                EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT),
                List.of("officer-employee-statement", "delivery-on-behalf-of-firm",
                    "delivery-on-behalf-of-individual", "verified-statement")),
            Arguments.of(PresenterType.ACSP, AssociationStatementType.ACSP_STATEMENT,
                EnumSet.noneOf(VerificationStatementType.class),
                List.of("acsp-statement", "delivery-on-behalf-of-firm", "delivery-on-behalf-of-individual")),
            Arguments.of(PresenterType.OFFICIAL_RECEIVER, AssociationStatementType.OFFICIAL_RECEIVER_STATEMENT,
                EnumSet.noneOf(VerificationStatementType.class),
                List.of("official-receiver-statement", "delivery-on-behalf-of-firm",
                    "delivery-on-behalf-of-individual")));
    }

    private static Stream<Arguments> singleDeliveryFormGroups() {
        return Stream.of(
            Arguments.of(FormType.AD01, FormGroup.FIRM_DELIVERY, DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM),
            Arguments.of(FormType.PSC01, FormGroup.INDIVIDUAL_DELIVERY,
                DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL));
    }
}

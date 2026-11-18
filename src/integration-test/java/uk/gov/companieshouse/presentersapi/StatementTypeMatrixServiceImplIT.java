package uk.gov.companieshouse.presentersapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;
import uk.gov.companieshouse.presentersapi.service.impl.StatementTypeMatrixServiceImpl;

/**
 * Verifies StatementTypeMatrixServiceImpl is wired with the BiMap beans populated from the reference data YAML files.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StatementTypeMatrixServiceImplIT {

    @Autowired
    private StatementTypeMatrixServiceImpl service;

    @Test
    void shouldCombinePresenterTypeAndFormGroupDeliveryStatements() {
        final var statements = service.statementTypesAllowedFor(PresenterType.OFFICER_EMPLOYEE,
                                                         FormType.MR01);

        assertThat(statements.associationStatements(),
                is(EnumSet.of(AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT)));
        assertThat(statements.deliveryStatements(), is(EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM,
                DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL)));
        assertThat(statements.verificationStatements(), is(EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT)));
    }

    @Test
    void shouldKeepValidCombinationsSeparateFromTheMergedStatementUnion() {
        final var union = service.statementTypesAllowedFor(PresenterType.OFFICER_EMPLOYEE, FormType.WU02);
        final var combinations = service.validStatementCombinationsFor(PresenterType.OFFICER_EMPLOYEE, FormType.WU02);

        assertThat(union.deliveryStatements(), is(EnumSet.of(
            DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM,
            DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL)));
        assertThat(combinations, is(Set.of(new PresenterStatements(
            EnumSet.of(AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT),
            EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM),
            EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT)))));
    }

    @Test
    void shouldResolveDeliveryStatementsFromFormGroupForMultiDeliveryForm() {
        final var statements = service.statementTypesAllowedFor(PresenterType.ACSP, FormType.MR01);

        assertThat(statements.associationStatements(), is(EnumSet.of(AssociationStatementType.ACSP_STATEMENT)));
        assertThat(statements.deliveryStatements(), is(EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM,
                DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL)));
        assertThat(statements.verificationStatements(), is(empty()));
    }

    @Test
    void shouldReturnNoDeliveryStatementsForIndividualFilingForSelf() {
        final var statements = service.statementTypesAllowedFor(PresenterType.INDIVIDUAL_FILING_FOR_SELF,
                                                         FormType.MR01);

        assertThat(statements.associationStatements(), is(empty()));
        assertThat(statements.deliveryStatements(), is(empty()));
        assertThat(statements.verificationStatements(), is(EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT)));
    }

    @Test
    void shouldRejectNullFormTypeWhenResolvingFormGroupDeliveryStatements() {
        assertThrows(NullPointerException.class, () -> service.statementTypesAllowedFor(
            PresenterType.INDIVIDUAL_REPRESENTING, null));
    }
}

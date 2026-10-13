package uk.gov.companieshouse.presentersapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;
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
        final var statements = service.statementTypesFor(PresenterType.OFFICER_EMPLOYEE,
                                                         FormType.PSC01);

        assertThat(statements.associationStatements(),
                is(EnumSet.of(AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT)));
        assertThat(statements.deliveryStatements(), is(EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM,
                DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL)));
        assertThat(statements.verificationStatements(), is(EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT)));
    }

    @Test
    void shouldResolveDeliveryStatementsFromFormGroupForMultiDeliveryForm() {
        final var statements = service.statementTypesFor(PresenterType.ACSP, FormType.MR01);

        assertThat(statements.associationStatements(), is(EnumSet.of(AssociationStatementType.ACSP_STATEMENT)));
        assertThat(statements.deliveryStatements(), is(EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM,
                DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL)));
        assertThat(statements.verificationStatements(), is(empty()));
    }

    @Test
    void shouldReturnNoDeliveryStatementsForIndividualFilingForSelf() {
        final var statements = service.statementTypesFor(PresenterType.INDIVIDUAL_FILING_FOR_SELF,
                                                         FormType.MR01);

        assertThat(statements.associationStatements(), is(empty()));
        assertThat(statements.deliveryStatements(), is(empty()));
        assertThat(statements.verificationStatements(), is(EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT)));
    }

    @Test
    void shouldOnlyUsePresenterTypeDeliveryStatementsWhenFormTypeIsNull() {
        final var statements = service.statementTypesFor(
                PresenterType.INDIVIDUAL_FILING_FOR_SOMEONE_ELSE, null);

        assertThat(statements.deliveryStatements(),
                is(EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL)));
    }
}

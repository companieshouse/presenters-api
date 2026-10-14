package uk.gov.companieshouse.presentersapi.model.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;

class PresenterStatementsTest {

    private final EnumSet<AssociationStatementType> association =
        EnumSet.of(AssociationStatementType.ACSP_STATEMENT);
    private final EnumSet<DeliveryStatementType> delivery =
        EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM);
    private final EnumSet<VerificationStatementType> verification =
        EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT);

    @Test
    void exposesTheSuppliedStatements() {
        final var statements = new PresenterStatements(association, delivery, verification);

        assertThat(statements.associationStatements(), contains(AssociationStatementType.ACSP_STATEMENT));
        assertThat(statements.deliveryStatements(), contains(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM));
        assertThat(statements.verificationStatements(), contains(VerificationStatementType.VERIFIED_STATEMENT));
    }

    @Test
    void acceptsEmptySets() {
        final var statements = new PresenterStatements(
            EnumSet.noneOf(AssociationStatementType.class),
            EnumSet.noneOf(DeliveryStatementType.class),
            EnumSet.noneOf(VerificationStatementType.class));

        assertThat(statements.associationStatements(), is(empty()));
        assertThat(statements.deliveryStatements(), is(empty()));
        assertThat(statements.verificationStatements(), is(empty()));
    }

    @Test
    void isNotAffectedByLaterChangesToTheSuppliedSets() {
        final var statements = new PresenterStatements(association, delivery, verification);

        association.clear();

        assertThat(statements.associationStatements(), contains(AssociationStatementType.ACSP_STATEMENT));
    }

    @Test
    void isNotAffectedByChangesToTheReturnedSets() {
        final var statements = new PresenterStatements(association, delivery, verification);

        final var returned = statements.deliveryStatements();
        returned.clear();

        assertThat(statements.deliveryStatements(), is(not(empty())));
        assertThat(statements.deliveryStatements(), is(not(sameInstance(returned))));
    }

    @Test
    void rejectsNullSets() {
        assertThrows(NullPointerException.class, () -> new PresenterStatements(null, delivery, verification));
        assertThrows(NullPointerException.class, () -> new PresenterStatements(association, null, verification));
        assertThrows(NullPointerException.class, () -> new PresenterStatements(association, delivery, null));
    }

    @Test
    void comparesByValue() {
        final var first = new PresenterStatements(association, delivery, verification);
        final var second = new PresenterStatements(
            EnumSet.copyOf(association), EnumSet.copyOf(delivery), EnumSet.copyOf(verification));
        final var different = new PresenterStatements(
            EnumSet.noneOf(AssociationStatementType.class), delivery, verification);

        assertThat(first, is(equalTo(second)));
        assertThat(first.hashCode(), is(second.hashCode()));
        assertThat(first, is(not(equalTo(different))));
    }

    @Test
    void statementCodesCombinesCategoriesInEnumDeclarationOrder() {
        final var statements = new PresenterStatements(
            EnumSet.of(AssociationStatementType.ACSP_STATEMENT, AssociationStatementType.OFFICER_EMPLOYEE_STATEMENT),
            EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_INDIVIDUAL,
                DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM),
            verification);

        assertThat(statements.statementCodes(), contains(
            "officer-employee-statement",
            "acsp-statement",
            "delivery-on-behalf-of-firm",
            "delivery-on-behalf-of-individual",
            "verified-statement"));
    }

    @Test
    void statementCodesReturnsEmptyListWhenAllCategoriesAreEmpty() {
        final var statements = new PresenterStatements(
            EnumSet.noneOf(AssociationStatementType.class),
            EnumSet.noneOf(DeliveryStatementType.class),
            EnumSet.noneOf(VerificationStatementType.class));

        assertThat(statements.statementCodes(), is(empty()));
    }

    @ParameterizedTest
    @EnumSource(StatementCategory.class)
    void statementCodesHandlesOnlyOnePopulatedCategory(final StatementCategory category) {
        final var statements = new PresenterStatements(
            category == StatementCategory.ASSOCIATION ? association : EnumSet.noneOf(AssociationStatementType.class),
            category == StatementCategory.DELIVERY ? delivery : EnumSet.noneOf(DeliveryStatementType.class),
            category == StatementCategory.VERIFICATION ? verification : EnumSet.noneOf(VerificationStatementType.class));

        final var expectedCode = switch (category) {
            case ASSOCIATION -> "acsp-statement";
            case DELIVERY -> "delivery-on-behalf-of-firm";
            case VERIFICATION -> "verified-statement";
        };
        assertThat(statements.statementCodes(), contains(expectedCode));
    }

    @Test
    void statementCodesReturnsUnmodifiableList() {
        final var statements = new PresenterStatements(association, delivery, verification);
        final var codes = statements.statementCodes();

        assertThrows(UnsupportedOperationException.class, () -> codes.add("new-statement"));
        assertThrows(UnsupportedOperationException.class, () -> codes.set(0, "replacement-statement"));
        assertThrows(UnsupportedOperationException.class, codes::clear);
    }

    @Test
    void statementCodesIsNotAffectedByChangesToSuppliedOrReturnedSets() {
        final var statements = new PresenterStatements(association, delivery, verification);

        association.clear();
        delivery.clear();
        verification.clear();
        statements.associationStatements().clear();
        statements.deliveryStatements().clear();
        statements.verificationStatements().clear();

        assertThat(statements.statementCodes(), contains(
            "acsp-statement", "delivery-on-behalf-of-firm", "verified-statement"));
    }

    private enum StatementCategory {
        ASSOCIATION, DELIVERY, VERIFICATION
    }
}

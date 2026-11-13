package uk.gov.companieshouse.presentersapi.guard;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;
import uk.gov.companieshouse.presentersapi.error.RequestBodyErrors;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;
import uk.gov.companieshouse.service.rest.err.Err;

class RequestListGuardTest {

    private enum UnexpectedStatementType {
        UNEXPECTED
    }

    private static final String ASSOCIATION = AssociationStatementType.ACSP_STATEMENT.code();
    private static final String DELIVERY = DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM.code();
    private static final String VERIFICATION = VerificationStatementType.VERIFIED_STATEMENT.code();
    private static final String DATA_TYPE = DataObjectType.values()[0].code();
    private static final String OTHER_DATA_TYPE = DataObjectType.values()[1].code();

    private static void assertStatementError(final List<String> statementIds, final Err expected) {
        assertThat(RequestListGuard.guardStatements(statementIds).errors(), is(List.of(expected)));
    }

    private static void assertDataObjectError(final List<String> dataTypes, final Err expected) {
        assertThat(RequestListGuard.guardDataObjectTypes(dataTypes).errors(), is(List.of(expected)));
    }

    @Test
    void shouldGroupStatementsByCategoryWhenAllStatementsValid() {
        final var result = RequestListGuard.guardStatements(List.of(VERIFICATION, DELIVERY, ASSOCIATION));

        assertThat(result.isValid(), is(true));
        assertThat(result.value(), is(new PresenterStatements(
            EnumSet.of(AssociationStatementType.ACSP_STATEMENT),
            EnumSet.of(DeliveryStatementType.DELIVERY_ON_BEHALF_OF_FIRM),
            EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT))));
    }

    @Test
    void shouldThrowWhenResolvedStatementHasUnexpectedType() {
        final var statements = List.<Enum<?>>of(UnexpectedStatementType.UNEXPECTED);
        final var exception = assertThrows(IllegalStateException.class,
            () -> RequestListGuard.toPresenterStatements(statements));

        assertThat(exception.getMessage(), is("Unexpected statement type UNEXPECTED"));
    }

    @Test
    void shouldReturnMissingErrorWhenStatementsNull() {
        assertStatementError(null, RequestBodyErrors.statementTypesMissing());
    }

    @Test
    void shouldReturnMissingErrorWhenStatementsEmpty() {
        assertStatementError(List.of(), RequestBodyErrors.statementTypesMissing());
    }

    @Test
    void shouldReturnNotValidErrorWhenStatementCodeUnknown() {
        assertStatementError(List.of(ASSOCIATION, "no-such-statement"),
            RequestBodyErrors.statementTypesNotValid("no-such-statement"));
    }

    @Test
    void shouldReturnNotValidErrorWhenStatementElementNull() {
        assertStatementError(Arrays.asList(ASSOCIATION, null), RequestBodyErrors.statementTypesNotValid("null"));
    }

    @Test
    void shouldReturnDuplicatedErrorWhenStatementRepeated() {
        assertStatementError(List.of(DELIVERY, ASSOCIATION, DELIVERY),
            RequestBodyErrors.statementTypesDuplicated(DELIVERY));
    }

    @Test
    void shouldReturnSingleDuplicatedErrorWhenStatementRepeatedThreeTimes() {
        assertStatementError(List.of(DELIVERY, DELIVERY, DELIVERY),
            RequestBodyErrors.statementTypesDuplicated(DELIVERY));
    }

    @Test
    void shouldReportNotValidRatherThanDuplicatedWhenStatementsHaveUnknownAndDuplicate() {
        assertStatementError(List.of(DELIVERY, DELIVERY, "no-such-statement"),
                             RequestBodyErrors.statementTypesNotValid("no-such-statement"));
    }

    @Test
    void shouldReportNotValidRatherThanDuplicatedWhenUnknownStatementRepeated() {
        assertStatementError(List.of("no-such-statement", "no-such-statement"),
                             RequestBodyErrors.statementTypesNotValid("no-such-statement"));
    }

    @Test
    void shouldIdentifyEachDistinctInvalidStatementWhenSeveralAreRejected() {
        final var result = RequestListGuard.guardStatements(
            Arrays.asList("unknown-first", DELIVERY, null, "unknown-second", "unknown-first", null, DELIVERY));

        assertThat(result.errors(), is(List.of(
            RequestBodyErrors.statementTypesNotValid("unknown-first, null, unknown-second"))));
    }

    @Test
    void shouldIdentifyEachDuplicatedStatementInFirstOccurrenceOrder() {
        final var result = RequestListGuard.guardStatements(
            List.of(DELIVERY, ASSOCIATION, ASSOCIATION, DELIVERY, ASSOCIATION));

        assertThat(result.errors(), is(List.of(
            RequestBodyErrors.statementTypesDuplicated(DELIVERY + ", " + ASSOCIATION))));
    }

    @Test
    void shouldResolveDataObjectTypesWhenAllValid() {
        final var result = RequestListGuard.guardDataObjectTypes(List.of(DATA_TYPE, OTHER_DATA_TYPE));

        assertThat(result.isValid(), is(true));
        assertThat(result.value(), is(EnumSet.of(DataObjectType.values()[0], DataObjectType.values()[1])));
    }

    @Test
    void shouldReturnMissingErrorWhenDataObjectTypesNull() {
        assertDataObjectError(null, RequestBodyErrors.dataObjectTypesMissing());
    }

    @Test
    void shouldReturnMissingErrorWhenDataObjectTypesEmpty() {
        assertDataObjectError(List.of(), RequestBodyErrors.dataObjectTypesMissing());
    }

    @Test
    void shouldReturnNotValidErrorWhenDataObjectTypeUnknown() {
        assertDataObjectError(List.of(DATA_TYPE, "no-such-data-type"),
            RequestBodyErrors.dataObjectTypesNotValid("no-such-data-type"));
    }

    @Test
    void shouldReturnNotValidErrorWhenDataObjectTypeElementNull() {
        assertDataObjectError(Arrays.asList(null, DATA_TYPE), RequestBodyErrors.dataObjectTypesNotValid("null"));
    }

    @Test
    void shouldReturnDuplicatedErrorWhenDataObjectTypeRepeated() {
        assertDataObjectError(List.of(DATA_TYPE, OTHER_DATA_TYPE, DATA_TYPE),
                              RequestBodyErrors.dataObjectTypesDuplicated(DATA_TYPE));
    }

    @Test
    void shouldReportNotValidRatherThanDuplicatedWhenDataObjectTypesHaveUnknownAndDuplicate() {
        assertDataObjectError(List.of(DATA_TYPE, DATA_TYPE, "no-such-data-type"),
                              RequestBodyErrors.dataObjectTypesNotValid("no-such-data-type"));
    }

    @Test
    void shouldIdentifyEachDistinctInvalidDataObjectTypeWhenSeveralAreRejected() {
        final var result = RequestListGuard.guardDataObjectTypes(
            Arrays.asList("unknown-first", DATA_TYPE, null, "unknown-second", "unknown-first", null, DATA_TYPE));

        assertThat(result.errors(), is(List.of(
            RequestBodyErrors.dataObjectTypesNotValid("unknown-first, null, unknown-second"))));
    }

    @Test
    void shouldIdentifyEachDuplicatedDataObjectTypeInFirstOccurrenceOrder() {
        final var result = RequestListGuard.guardDataObjectTypes(
            List.of(DATA_TYPE, OTHER_DATA_TYPE, OTHER_DATA_TYPE, DATA_TYPE, DATA_TYPE));

        assertThat(result.errors(), is(List.of(
            RequestBodyErrors.dataObjectTypesDuplicated(DATA_TYPE + ", " + OTHER_DATA_TYPE))));
    }

}

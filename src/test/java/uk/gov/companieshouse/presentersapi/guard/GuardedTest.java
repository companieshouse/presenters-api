package uk.gov.companieshouse.presentersapi.guard;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;

class GuardedTest {

    private static final Err FIRST = GuardErrors.invalid(ErrorLocation.BODY, "first", "First is invalid");
    private static final Err SECOND = GuardErrors.invalid(ErrorLocation.BODY, "second", "Second is invalid");

    @Test
    void shouldHoldValueAndNoErrorsWhenValid() {
        final var result = Guarded.valid("value");

        assertThat(result.isValid(), is(true));
        assertThat(result.value(), is("value"));
        assertThat(result.errors(), is(List.of()));
    }

    @Test
    void shouldHoldErrorsAndRefuseValueWhenInvalid() {
        final var result = Guarded.<String>invalid(FIRST);

        assertThat(result.isValid(), is(false));
        assertThat(result.errors(), is(List.of(FIRST)));
        final var exception = assertThrows(IllegalStateException.class, result::value);
        assertThat(exception.getMessage().startsWith("No value"), is(true));
    }

    @Test
    void shouldRejectEmptyErrorListWhenCreatingInvalid() {
        final var emptyErrors = List.<Err>of();

        assertThrows(IllegalArgumentException.class, () -> Guarded.invalid(emptyErrors));
    }

    @Test
    void shouldRejectNullValueWhenCreatingValid() {
        assertThrows(NullPointerException.class, () -> Guarded.valid(null));
    }

    @Test
    void shouldRejectNullErrorWhenCreatingInvalid() {
        assertThrows(NullPointerException.class, () -> Guarded.invalid((Err) null));
    }

    @Test
    void shouldCopyAndProtectErrorsWhenCreatingInvalidFromList() {
        final var errors = new ArrayList<>(List.of(FIRST, SECOND));
        final var result = Guarded.<String>invalid(errors);
        errors.clear();
        final var returnedErrors = result.errors();

        assertThat(returnedErrors, is(List.of(FIRST, SECOND)));
        assertThrows(UnsupportedOperationException.class, returnedErrors::clear);
    }

    @Test
    void shouldConvertValueWhenMappingValidResult() {
        assertThat(Guarded.valid(2).map(number -> number * 3), is(Guarded.valid(6)));
    }

    @Test
    void shouldKeepErrorsAndSkipMapperWhenMappingInvalidResult() {
        final var result = Guarded.<Integer>invalid(FIRST).map(number -> {
            throw new AssertionError("mapper must not run");
        });

        assertThat(result.errors(), is(List.of(FIRST)));
    }

    @Test
    void shouldApplyFurtherRuleWhenFlatMappingValidResult() {
        final var result = Guarded.valid("x").flatMap(value -> Guarded.<String>invalid(SECOND));

        assertThat(result.errors(), is(List.of(SECOND)));
    }

    @Test
    void shouldKeepFirstErrorsAndSkipRuleWhenFlatMappingInvalidResult() {
        final var result = Guarded.<String>invalid(FIRST).flatMap(value -> {
            throw new AssertionError("rule must not run");
        });

        assertThat(result.errors(), is(List.of(FIRST)));
    }

    @Test
    void shouldCollectErrorsOfInvalidResultsInOrderWhenCombining() {
        final var errors = Guarded.combinedErrors(
            Guarded.<String>invalid(FIRST), Guarded.valid("ok"), Guarded.<String>invalid(SECOND));

        assertThat(errors.size(), is(2));
        assertThat(errors.containsError(FIRST), is(true));
        assertThat(errors.containsError(SECOND), is(true));
    }

    @Test
    void shouldHaveNoErrorsWhenCombiningOnlyValidResults() {
        assertThat(Guarded.combinedErrors(Guarded.valid("a"), Guarded.valid("b")).hasErrors(), is(false));
    }

    @Test
    void shouldHaveNoErrorsWhenCombiningNoResults() {
        assertThat(Guarded.combinedErrors().hasErrors(), is(false));
    }

    @Test
    void shouldReturnFoundResultWhenConvertingValidResult() {
        final var serviceResult = Guarded.valid("value").toServiceResult();

        assertThat(serviceResult.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(serviceResult.getData(), is("value"));
    }

    @Test
    void shouldReturnValidationErrorResultWhenConvertingInvalidResult() {
        final var serviceResult = Guarded.<String>invalid(List.of(FIRST, SECOND)).toServiceResult();

        assertThat(serviceResult.getStatus(), is(ServiceResultStatus.VALIDATION_ERROR));
        assertThat(serviceResult.getErrors().size(), is(2));
    }

    @Test
    void shouldMeetEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(Guarded.class)
            .withNonnullFields("errors")
            .withPrefabValues(Err.class, FIRST, SECOND)
            .verify();
    }

    @Test
    void shouldDescribeValidAndInvalidResults() {
        assertThat(Guarded.valid("value").toString(), is("Valid[value]"));
        assertThat(Guarded.<String>invalid(FIRST).toString(), is("Invalid" + List.of(FIRST)));
    }
}

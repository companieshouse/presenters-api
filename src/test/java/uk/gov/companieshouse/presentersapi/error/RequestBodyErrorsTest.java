package uk.gov.companieshouse.presentersapi.error;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.companieshouse.service.rest.err.Err;

class RequestBodyErrorsTest {

    static Stream<Arguments> listErrors() {
        return Stream.of(
            listCase("statement types missing", RequestBodyErrors::statementTypesMissing,
                     "statement_ids", "No Statement Types have been supplied"),
            listCase("statement types not valid", () -> RequestBodyErrors.statementTypesNotValid("NOPE"),
                     "statement_ids", "One or more of the statements are not valid"),
            listCase("statement types duplicated", () -> RequestBodyErrors.statementTypesDuplicated("NOPE"),
                     "statement_ids", "Duplicate Statement Types have been supplied"),
            listCase("data object types missing", RequestBodyErrors::dataObjectTypesMissing,
                     "data_types", "No Data Object Types have been supplied"),
            listCase("data object types not valid", () -> RequestBodyErrors.dataObjectTypesNotValid("NOPE"),
                     "data_types", "One or more of the data object types are not valid"),
            listCase("data object types duplicated", () -> RequestBodyErrors.dataObjectTypesDuplicated("NOPE"),
                     "data_types", "Duplicate Data Object Types have been supplied"));
    }

    private static Arguments listCase(final String name, final Supplier<Err> error, final String location,
                                      final String message) {
        return Arguments.of(name, error, location, message);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("listErrors")
    void shouldLocateListErrorInBodyWithRejectedValueUnlessMissingWhen(
            final String name, final Supplier<Err> error, final String location, final String message) {
        final var err = error.get();

        assertThat(err.getLocation(), is(location));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_BODY));
        assertThat(err.getType(), is(Err.ERR_TYPE_VALIDATION));
        assertThat(err.getError(), is(message));
        assertThat(err.getErrorValues(), is(name.endsWith("missing") ? Map.of() : Map.of("rejected", "NOPE")));
    }

    @Test
    void shouldReturnRequiredMessageWithoutRejectedValueWhenFormTypeMissing() {
        final var err = RequestBodyErrors.formTypeRequired();

        assertThat(err.getLocation(), is("form_type"));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_BODY));
        assertThat(err.getError(), is("Form type is required"));
        assertThat(err.getErrorValues(), is(Map.of()));
    }

    @Test
    void shouldCarryRejectedValueWhenFormTypeInvalid() {
        final var err = RequestBodyErrors.formTypeInvalid("NOPE");

        assertThat(err.getLocation(), is("form_type"));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_BODY));
        assertThat(err.getError(), is("Form type is invalid"));
        assertThat(err.getErrorValues(), is(Map.of("rejected", "NOPE")));
    }

    @Test
    void shouldCarryFormAndGroupWhenFormTypeNotSupported() {
        final var err = RequestBodyErrors.formTypeNotSupported("WU12", "exempt-form");

        assertThat(err.getLocation(), is("form_type"));
        assertThat(err.getError(),
            is("Form WU12 not supported because it belongs to the form group exempt-form"));
        assertThat(err.getErrorValues(), is(Map.of("rejected", "WU12", "form_group", "exempt-form")));
    }
}

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

    private static final String FORM_TYPE_LOCATION = "$.form_type";
    private static final String STATEMENT_IDS_LOCATION = "$.statement_ids";
    private static final String DATA_TYPES_LOCATION = "$.data_types";

    static Stream<Arguments> listErrors() {
        return Stream.of(
            listCase("statement types missing", RequestBodyErrors::statementTypesMissing,
                     STATEMENT_IDS_LOCATION, ErrorMessages.STATEMENT_IDS_REQUIRED),
            listCase("statement types not valid", () -> RequestBodyErrors.statementTypesNotValid("NOPE"),
                     STATEMENT_IDS_LOCATION, ErrorMessages.STATEMENT_IDS_NOT_VALID),
            listCase("statement types duplicated", () -> RequestBodyErrors.statementTypesDuplicated("NOPE"),
                     STATEMENT_IDS_LOCATION, ErrorMessages.STATEMENT_IDS_DUPLICATE),
            listCase("data object types missing", RequestBodyErrors::dataObjectTypesMissing,
                     DATA_TYPES_LOCATION, ErrorMessages.DATA_OBJECTS_REQUIRED),
            listCase("data object types not valid", () -> RequestBodyErrors.dataObjectTypesNotValid("NOPE"),
                     DATA_TYPES_LOCATION, ErrorMessages.DATA_OBJECTS_NOT_VALID),
            listCase("data object types duplicated", () -> RequestBodyErrors.dataObjectTypesDuplicated("NOPE"),
                     DATA_TYPES_LOCATION, ErrorMessages.DATA_OBJECTS_DUPLICATE));
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

        assertThat(err.getLocation(), is(FORM_TYPE_LOCATION));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_BODY));
        assertThat(err.getError(), is(ErrorMessages.FORM_TYPE_REQUIRED));
        assertThat(err.getErrorValues(), is(Map.of()));
    }

    @Test
    void shouldCarryRejectedValueWhenFormTypeInvalid() {
        final var err = RequestBodyErrors.formTypeInvalid("NOPE");

        assertThat(err.getLocation(), is(FORM_TYPE_LOCATION));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_BODY));
        assertThat(err.getError(), is(ErrorMessages.FORM_TYPE_INVALID));
        assertThat(err.getErrorValues(), is(Map.of("rejected", "NOPE")));
    }

    @Test
    void shouldCarryFormAndGroupWhenFormTypeNotSupported() {
        final var err = RequestBodyErrors.formTypeNotSupported("WU12", "exempt-form");

        assertThat(err.getLocation(), is(FORM_TYPE_LOCATION));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_BODY));
        assertThat(err.getType(), is(Err.ERR_TYPE_VALIDATION));
        assertThat(err.getError(),
            is(ErrorMessages.FORM_TYPE_NOT_SUPPORTED.formatted("WU12", "exempt-form")));
        assertThat(err.getErrorValues(), is(Map.of("rejected", "WU12", "form_group", "exempt-form")));
    }
}

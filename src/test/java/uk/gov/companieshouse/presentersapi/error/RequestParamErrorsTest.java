package uk.gov.companieshouse.presentersapi.error;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.service.rest.err.Err;

class RequestParamErrorsTest {

    @Test
    void shouldLocateRequiredMessageAtQueryParameterWithoutRejectedValueWhenFormTypeMissing() {
        final var err = RequestParamErrors.formTypeRequired();

        assertThat(err.getLocation(), is("form_type"));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_QUERY));
        assertThat(err.getType(), is(Err.ERR_TYPE_VALIDATION));
        assertThat(err.getError(), is(ErrorMessages.FORM_TYPE_REQUIRED));
        assertThat(err.getErrorValues(), is(Map.of()));
    }

    @Test
    void shouldCarryRejectedValueAtQueryParameterWhenFormTypeInvalid() {
        final var err = RequestParamErrors.formTypeInvalid("NOPE");

        assertThat(err.getLocation(), is("form_type"));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_QUERY));
        assertThat(err.getType(), is(Err.ERR_TYPE_VALIDATION));
        assertThat(err.getError(), is(ErrorMessages.FORM_TYPE_INVALID));
        assertThat(err.getErrorValues(), is(Map.of("rejected", "NOPE")));
    }

    @Test
    void shouldCarryFormAndGroupAtQueryParameterWhenFormTypeNotSupported() {
        final var err = RequestParamErrors.formTypeNotSupported("WU12", "exempt-form");

        assertThat(err.getLocation(), is("form_type"));
        assertThat(err.getLocationType(), is(Err.LOCATION_TYPE_QUERY));
        assertThat(err.getType(), is(Err.ERR_TYPE_VALIDATION));
        assertThat(err.getError(), is(ErrorMessages.FORM_TYPE_NOT_SUPPORTED.formatted("WU12", "exempt-form")));
        assertThat(err.getErrorValues(), is(Map.of("rejected", "WU12", "form_group", "exempt-form")));
    }

    @Test
    void shouldUseSameWordingAsBodyErrorsWhenFormTypeInvalidOrMissing() {
        assertThat(RequestParamErrors.formTypeRequired().getError(),
            is(RequestBodyErrors.formTypeRequired().getError()));
        assertThat(RequestParamErrors.formTypeInvalid("X").getError(),
            is(RequestBodyErrors.formTypeInvalid("X").getError()));
    }
}

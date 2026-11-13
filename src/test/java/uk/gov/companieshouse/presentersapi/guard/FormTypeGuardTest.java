package uk.gov.companieshouse.presentersapi.guard;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.service.rest.err.Err;

class FormTypeGuardTest {

    private enum FormGuard {
        BODY(FormTypeGuard::body, Err.LOCATION_TYPE_BODY),
        QUERY(FormTypeGuard::query, Err.LOCATION_TYPE_QUERY);

        private final Function<String, Guarded<FormType>> guard;
        private final String locationType;

        FormGuard(final Function<String, Guarded<FormType>> guard, final String locationType) {
            this.guard = guard;
            this.locationType = locationType;
        }
    }

    @ParameterizedTest
    @EnumSource(FormGuard.class)
    void shouldResolveKnownFormType(final FormGuard formGuard) {
        final var result = formGuard.guard.apply("AD01");

        assertThat(result.value(), is(FormType.AD01));
        assertThat(result.errors(), is(List.of()));
    }

    @ParameterizedTest
    @EnumSource(FormGuard.class)
    void shouldReportRequiredFormTypeWhenNullOrBlank(final FormGuard formGuard) {
        for (final var code : new String[] {null, "", "  "}) {
            final var result = formGuard.guard.apply(code);
            assertThat(result.errors().size(), is(1));
            final var error = result.errors().getFirst();
            assertThat(error.getLocation(), is("form_type"));
            assertThat(error.getLocationType(), is(formGuard.locationType));
            assertThat(error.getType(), is(Err.ERR_TYPE_VALIDATION));
            assertThat(error.getError(), is("Form type is required"));
            assertThat(error.getErrorValues(), is(Map.of()));
        }
    }

    @ParameterizedTest
    @EnumSource(FormGuard.class)
    void shouldReportRejectedFormTypeWhenUnknown(final FormGuard formGuard) {
        final var result = formGuard.guard.apply("unknown-form");

        assertThat(result.errors().size(), is(1));
        final var error = result.errors().getFirst();
        assertThat(error.getLocation(), is("form_type"));
        assertThat(error.getLocationType(), is(formGuard.locationType));
        assertThat(error.getType(), is(Err.ERR_TYPE_VALIDATION));
        assertThat(error.getError(), is("Form type is invalid"));
        assertThat(error.getErrorValues(), is(Map.of("rejected", "unknown-form")));
    }
}

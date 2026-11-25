package uk.gov.companieshouse.presentersapi.guard;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.FormGroup;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.service.rest.err.Err;

@ExtendWith(MockitoExtension.class)
class FormTypeGuardTest {

    @Mock
    private FormGroupFormTypeBiMap formGroupFormTypeBiMap;

    private enum FormGuard {
        BODY(FormTypeGuard::fromBody, Err.LOCATION_TYPE_BODY, "$.form_type"),
        QUERY(FormTypeGuard::fromQuery, Err.LOCATION_TYPE_QUERY, "form_type");

        private final BiFunction<FormTypeGuard, String, Guarded<FormType>> guard;
        private final String locationType;
        private final String location;

        FormGuard(final BiFunction<FormTypeGuard, String, Guarded<FormType>> guard, final String locationType,
            final String location) {
            this.guard = guard;
            this.locationType = locationType;
            this.location = location;
        }
    }

    @ParameterizedTest
    @EnumSource(FormGuard.class)
    void shouldResolveKnownFormTypeWhenGroupIsSupported(final FormGuard formGuard) {
        for (final var group : EnumSet.complementOf(EnumSet.of(FormGroup.UNMARKED, FormGroup.EXEMPT_FORM))) {
            when(formGroupFormTypeBiMap.formGroupFor(FormType.AD01)).thenReturn(group);
            final var result = formGuard.guard.apply(new FormTypeGuard(formGroupFormTypeBiMap), "AD01");

            assertThat(result.isValid(), is(true));
            assertThat(result.value(), is(FormType.AD01));
            assertThat(result.errors(), is(List.of()));
        }
    }

    @ParameterizedTest
    @EnumSource(FormGuard.class)
    void shouldReportFormAndGroupWhenFormGroupIsUnsupported(final FormGuard formGuard) {
        for (final var group : EnumSet.of(FormGroup.UNMARKED, FormGroup.EXEMPT_FORM)) {
            when(formGroupFormTypeBiMap.formGroupFor(FormType.AD01)).thenReturn(group);
            final var result = formGuard.guard.apply(new FormTypeGuard(formGroupFormTypeBiMap), "AD01");

            assertThat(result.isValid(), is(false));
            assertThat(result.errors().size(), is(1));
            final var error = result.errors().getFirst();
            assertThat(error.getLocation(), is(formGuard.location));
            assertThat(error.getLocationType(), is(formGuard.locationType));
            assertThat(error.getType(), is(Err.ERR_TYPE_VALIDATION));
            assertThat(error.getError(),
                is("Form AD01 not supported because it belongs to the form group: " + group.code()));
            assertThat(error.getErrorValues(), is(Map.of("rejected", "AD01", "form_group", group.code())));
        }
    }

    @ParameterizedTest
    @EnumSource(FormGuard.class)
    void shouldResolveKnownFormTypeWhenNoGroupIsMapped(final FormGuard formGuard) {
        final var guard = new FormTypeGuard(formGroupFormTypeBiMap);
        when(formGroupFormTypeBiMap.formGroupFor(FormType.WU15)).thenReturn(null);

        final var result = formGuard.guard.apply(guard, "WU15");

        assertThat(result.isValid(), is(true));
        assertThat(result.value(), is(FormType.WU15));
        assertThat(result.errors(), is(List.of()));
    }

    @ParameterizedTest
    @EnumSource(FormGuard.class)
    void shouldReportRequiredFormTypeWhenNullOrBlank(final FormGuard formGuard) {
        for (final var code : new String[] {null, "", "  "}) {
            final var result = formGuard.guard.apply(new FormTypeGuard(formGroupFormTypeBiMap), code);
            assertThat(result.errors().size(), is(1));
            final var error = result.errors().getFirst();
            assertThat(error.getLocation(), is(formGuard.location));
            assertThat(error.getLocationType(), is(formGuard.locationType));
            assertThat(error.getType(), is(Err.ERR_TYPE_VALIDATION));
            assertThat(error.getError(), is("Form type is required"));
            assertThat(error.getErrorValues(), is(Map.of()));
        }
        verifyNoInteractions(formGroupFormTypeBiMap);
    }

    @ParameterizedTest
    @EnumSource(FormGuard.class)
    void shouldReportRejectedFormTypeWhenUnknown(final FormGuard formGuard) {
        final var result = formGuard.guard.apply(new FormTypeGuard(formGroupFormTypeBiMap), "unknown-form");

        assertThat(result.errors().size(), is(1));
        final var error = result.errors().getFirst();
        assertThat(error.getLocation(), is(formGuard.location));
        assertThat(error.getLocationType(), is(formGuard.locationType));
        assertThat(error.getType(), is(Err.ERR_TYPE_VALIDATION));
        assertThat(error.getError(), is("Form type is invalid"));
        assertThat(error.getErrorValues(), is(Map.of("rejected", "unknown-form")));
        verifyNoInteractions(formGroupFormTypeBiMap);
    }
}

package uk.gov.companieshouse.presentersapi.guard;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.service.rest.err.Err;

class GuardsTest {

    private static final Err MISSING = GuardErrors.invalid(ErrorLocation.QUERY, "form_type", "missing");
    private static final Err INVALID = GuardErrors.invalid(ErrorLocation.QUERY, "form_type", "invalid");
    private static final Err DUPLICATED = GuardErrors.invalid(ErrorLocation.BODY, "types", "duplicated");

    private static Guarded<FormType> formType(final String code) {
        return Guards.requiredCode(code, FormType::find, () -> MISSING, () -> INVALID);
    }

    private static Guarded<List<PresenterType>> presenterTypes(final List<String> codes) {
        return Guards.codeList(codes, PresenterType::find, () -> MISSING,
            rejected -> listError(INVALID, rejected), rejected -> listError(DUPLICATED, rejected));
    }

    private static Err listError(final Err template, final String rejected) {
        return GuardErrors.invalid(ErrorLocation.BODY, "types", template.getError(), Map.of("rejected", rejected));
    }

    @ParameterizedTest
    @EnumSource(FormType.class)
    void shouldResolveConstantWhenCodeIsKnown(final FormType formType) {
        assertThat(formType(formType.code()), is(Guarded.valid(formType)));
    }

    @Test
    void shouldReturnMissingErrorWhenCodeIsNull() {
        assertThat(formType(null).errors(), is(List.of(MISSING)));
    }

    @Test
    void shouldReturnMissingErrorWhenCodeIsBlank() {
        assertThat(formType("  ").errors(), is(List.of(MISSING)));
    }

    @Test
    void shouldReturnInvalidErrorWhenCodeIsUnknown() {
        assertThat(formType("no-such-form").errors(), is(List.of(INVALID)));
    }

    @Test
    void shouldResolveConstantsInRequestOrderWhenCodesAreKnownAndDistinct() {
        final var result = presenterTypes(List.of("acsp", "officer-employee"));

        assertThat(result.value(), is(List.of(PresenterType.ACSP, PresenterType.OFFICER_EMPLOYEE)));
    }

    @Test
    void shouldResolveSingleCodeInAList() {
        assertThat(presenterTypes(List.of("acsp")).value(), is(List.of(PresenterType.ACSP)));
    }

    @Test
    void shouldProtectResolvedListFromModification() {
        final var result = presenterTypes(List.of("acsp"));
        final var value = result.value();

        assertThrows(UnsupportedOperationException.class, () -> value.add(PresenterType.OFFICER_EMPLOYEE));
    }

    @Test
    void shouldReturnMissingErrorWhenListIsNullOrEmpty() {
        assertThat(presenterTypes(null).errors(), is(List.of(MISSING)));
        assertThat(presenterTypes(List.of()).errors(), is(List.of(MISSING)));
    }

    @Test
    void shouldReturnNotValidErrorWhenListHasNullOrUnknownElement() {
        assertThat(presenterTypes(Arrays.asList("acsp", null)).errors(), is(List.of(listError(INVALID, "null"))));
        assertThat(presenterTypes(List.of("acsp", "no-such-presenter")).errors(),
            is(List.of(listError(INVALID, "no-such-presenter"))));
    }

    @Test
    void shouldReturnDuplicatedErrorWhenListRepeatsAConstant() {
        assertThat(presenterTypes(List.of("acsp", "officer-employee", "acsp")).errors(),
            is(List.of(listError(DUPLICATED, "acsp"))));
    }

    @Test
    void shouldPreferNotValidOverDuplicatedWhenListHasBoth() {
        assertThat(presenterTypes(List.of("acsp", "acsp", "no-such-presenter")).errors(),
            is(List.of(listError(INVALID, "no-such-presenter"))));
    }

    @Test
    void shouldReportEachDistinctInvalidCodeInFirstOccurrenceOrderWhenListAlsoHasDuplicates() {
        final var result = presenterTypes(Arrays.asList("unknown", "acsp", null, "other", "unknown", null, "acsp"));

        assertThat(result.errors(), is(List.of(listError(INVALID, "unknown, null, other"))));
        assertThat(result.errors().stream().map(Err::getErrorValues).toList(), is(List.of(
            Map.of("rejected", "unknown, null, other"))));
        assertThat(result.toServiceResult().getErrors().size(), is(1));
    }

    @Test
    void shouldReportEachDuplicatedConstantOnceInFirstOccurrenceOrder() {
        final var result = presenterTypes(List.of("acsp", "officer-employee", "officer-employee", "acsp", "acsp"));

        assertThat(result.errors(), is(List.of(listError(DUPLICATED, "acsp, officer-employee"))));
        assertThat(result.errors().stream().map(Err::getErrorValues).toList(), is(List.of(
            Map.of("rejected", "acsp, officer-employee"))));
        assertThat(result.toServiceResult().getErrors().size(), is(1));
    }

    @Test
    void shouldJudgeDuplicatesByResolvedConstantWhenDifferentCodesResolveToSameValue() {
        final var result = Guards.codeList(List.of("alias", "acsp"),
            code -> Optional.of(PresenterType.ACSP), () -> MISSING,
            rejected -> listError(INVALID, rejected), rejected -> listError(DUPLICATED, rejected));

        assertThat(result.errors(), is(List.of(listError(DUPLICATED, "alias"))));
    }

    @Test
    void shouldPreserveRejectedCodesWhenJoiningValues() {
        final var result = presenterTypes(List.of("quote\"code", "slash\\code", "line\ncode", "quote\"code"));

        assertThat(result.errors().size(), is(1));
        assertThat(result.errors().getFirst().getErrorValues(),
            is(Map.of("rejected", "quote\"code, slash\\code, line\ncode")));
    }

    @Test
    void shouldBuildErrorsAtEachLocationType() {
        assertThat(GuardErrors.invalid(ErrorLocation.BODY, "f", "m").getLocationType(), is(Err.LOCATION_TYPE_BODY));
        assertThat(GuardErrors.invalid(ErrorLocation.QUERY, "f", "m").getLocationType(), is(Err.LOCATION_TYPE_QUERY));
        assertThat(GuardErrors.invalid(ErrorLocation.PATH, "f", "m").getLocationType(), is(Err.LOCATION_TYPE_PATH));
    }

    @Test
    void shouldCarryErrorValuesWhenGivenThem() {
        final var err = GuardErrors.invalid(ErrorLocation.QUERY, "form_type", "Form type is invalid",
            Map.of("rejected", "NOPE"));

        assertThat(err.getLocation(), is("form_type"));
        assertThat(err.getType(), is(Err.ERR_TYPE_VALIDATION));
        assertThat(err.getError(), is("Form type is invalid"));
        assertThat(err.getErrorValues(), is(Map.of("rejected", "NOPE")));
    }
}

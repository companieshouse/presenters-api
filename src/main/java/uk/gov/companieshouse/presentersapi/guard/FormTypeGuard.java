package uk.gov.companieshouse.presentersapi.guard;

import java.util.function.BiFunction;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.error.RequestBodyErrors;
import uk.gov.companieshouse.presentersapi.error.RequestParamErrors;
import uk.gov.companieshouse.service.rest.err.Err;

/**
 * Resolves a required, supported form type code with errors at its request location. Body errors use JSONPath;
 * query errors use the query parameter name. Known form types without a group mapping remain valid.
 */
@Component
public final class FormTypeGuard {

    private final FormGroupFormTypeBiMap formGroupFormTypeBiMap;

    public FormTypeGuard(final FormGroupFormTypeBiMap formGroupFormTypeBiMap) {
        this.formGroupFormTypeBiMap = formGroupFormTypeBiMap;
    }

    /**
     * @param code the form type code from the request body, may be null
     * @return the recognised, supported form type or a request body error located at {@code $.form_type}
     */
    public Guarded<FormType> fromBody(final String code) {
        return Guards.requiredCode(code, FormType::find,
            RequestBodyErrors::formTypeRequired, () -> RequestBodyErrors.formTypeInvalid(code))
            .flatMap(formType -> supportedFormType(formType, RequestBodyErrors::formTypeNotSupported));
    }

    /**
     * @param code the form type code from a query parameter, may be null
     * @return the recognised, supported form type or a query parameter error located at {@code form_type}
     */
    public Guarded<FormType> fromQuery(final String code) {
        return Guards.requiredCode(code, FormType::find,
            RequestParamErrors::formTypeRequired, () -> RequestParamErrors.formTypeInvalid(code))
            .flatMap(formType -> supportedFormType(formType, RequestParamErrors::formTypeNotSupported));
    }

    private Guarded<FormType> supportedFormType(final FormType formType,
        final BiFunction<String, String, Err> unsupportedError) {
        final var formGroup = formGroupFormTypeBiMap.formGroupFor(formType);
        return switch (formGroup) {
            case UNMARKED, EXEMPT_FORM -> Guarded.invalid(unsupportedError.apply(formType.code(), formGroup.code()));
            case null, default -> Guarded.valid(formType);
        };
    }
}

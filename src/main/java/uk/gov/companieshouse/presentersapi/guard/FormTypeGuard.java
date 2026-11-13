package uk.gov.companieshouse.presentersapi.guard;

import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.error.RequestBodyErrors;
import uk.gov.companieshouse.presentersapi.error.RequestParamErrors;

/**
 * Resolves a required form type code with errors at its request location.
 */
public final class FormTypeGuard {

    private FormTypeGuard() {
    }

    /**
     * @param code the form type code from the request body, may be null
     * @return the recognised form type or a request body error
     */
    public static Guarded<FormType> body(final String code) {
        return Guards.requiredCode(code, FormType::find,
            RequestBodyErrors::formTypeRequired, () -> RequestBodyErrors.formTypeInvalid(code));
    }

    /**
     * @param code the form type code from a query parameter, may be null
     * @return the recognised form type or a query parameter error
     */
    public static Guarded<FormType> query(final String code) {
        return Guards.requiredCode(code, FormType::find,
            RequestParamErrors::formTypeRequired, () -> RequestParamErrors.formTypeInvalid(code));
    }
}

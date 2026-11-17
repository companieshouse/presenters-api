package uk.gov.companieshouse.presentersapi.error;

import java.util.Map;
import uk.gov.companieshouse.presentersapi.guard.ErrorLocation;
import uk.gov.companieshouse.presentersapi.guard.GuardErrors;
import uk.gov.companieshouse.service.rest.err.Err;

/**
 * Builds the library {@link Err} instances for request query parameter guard failures. Companion to
 * {@link RequestBodyErrors}: the wording matches, but every error is located at a query parameter.
 */
public final class RequestParamErrors {

    static final String FORM_TYPE_PARAM = "form_type";
    static final String REJECTED_KEY = "rejected";
    static final String FORM_GROUP_KEY = "form_group";

    private RequestParamErrors() {
    }

    /**
     * @return an error for a missing form type query parameter, with no rejected value
     */
    public static Err formTypeRequired() {
        return GuardErrors.invalid(ErrorLocation.QUERY, FORM_TYPE_PARAM, ErrorMessages.FORM_TYPE_REQUIRED);
    }

    /**
     * @param rejected the form type code that was not recognised
     * @return an error for an unknown form type query parameter, carrying the rejected value
     */
    public static Err formTypeInvalid(final String rejected) {
        return GuardErrors.invalid(ErrorLocation.QUERY, FORM_TYPE_PARAM, ErrorMessages.FORM_TYPE_INVALID,
                                        Map.of(REJECTED_KEY, rejected));
    }

    /**
     * @param formType the recognised form type code that cannot be processed
     * @param formGroup the code of the form group the form belongs to
     * @return a query parameter error carrying the rejected form type and its unsupported group
     */
    public static Err formTypeNotSupported(final String formType, final String formGroup) {
        return GuardErrors.invalid(ErrorLocation.QUERY, FORM_TYPE_PARAM,
                                   ErrorMessages.FORM_TYPE_NOT_SUPPORTED.formatted(formType, formGroup),
            Map.of(REJECTED_KEY, formType, FORM_GROUP_KEY, formGroup));
    }
}

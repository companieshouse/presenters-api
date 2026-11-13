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

    private RequestParamErrors() {
    }

    /**
     * @return an error for a missing form type query parameter, with no rejected value
     */
    public static Err formTypeRequired() {
        return GuardErrors.invalid(ErrorLocation.QUERY, FORM_TYPE_PARAM, "Form type is required");
    }

    /**
     * @param rejected the form type code that was not recognised
     * @return an error for an unknown form type query parameter, carrying the rejected value
     */
    public static Err formTypeInvalid(final String rejected) {
        return GuardErrors.invalid(ErrorLocation.QUERY, FORM_TYPE_PARAM, "Form type is invalid",
                                        Map.of(REJECTED_KEY, rejected));
    }
}

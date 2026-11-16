package uk.gov.companieshouse.presentersapi.guard;

import java.util.Map;
import uk.gov.companieshouse.service.rest.err.Err;

/**
 * Builds guard {@link Err}s, so that wording, structure and rejected-value handling stay
 * consistent. Call-site-specific messages belong in thin wrappers around this class.
 */
public final class GuardErrors {

    private GuardErrors() {
    }

    /**
     * @param location where the field is in the request
     * @param field    the name of the invalid field
     * @param message  why the field is not valid
     * @return a guard error with no error values
     */
    public static Err invalid(final ErrorLocation location, final String field, final String message) {
        return location.builderFor(field).withError(message).build();
    }

    /**
     * @param location    where the field is in the request
     * @param field       the name of the invalid field
     * @param message     why the field is not valid
     * @param errorValues extra values to report, such as the rejected value
     * @return a guard error carrying the error values
     */
    public static Err invalid(final ErrorLocation location, final String field, final String message,
                              final Map<String, String> errorValues) {
        return location.builderFor(field).withError(message).withErrorValues(errorValues).build();
    }
}

package uk.gov.companieshouse.presentersapi.error;

import java.util.Map;
import uk.gov.companieshouse.presentersapi.guard.ErrorLocation;
import uk.gov.companieshouse.presentersapi.guard.GuardErrors;
import uk.gov.companieshouse.service.rest.err.Err;

/**
 * Builds the library {@link Err} instances for request body guard failures, so that the same wording and
 * location apply wherever the request is guarded. All errors are located in the request body, using the
 * JSON property names of the validation request.
 */
public final class RequestBodyErrors {

    static final String FORM_TYPE_LOCATION = "form_type";
    static final String STATEMENT_IDS_LOCATION = "statement_ids";
    static final String DATA_TYPES_LOCATION = "data_types";

    static final String REJECTED_KEY = "rejected";
    static final String FORM_GROUP_KEY = "form_group";

    private RequestBodyErrors() {
    }

    /**
     * @return an error for a missing form type, with no rejected value
     */
    public static Err formTypeRequired() {
        return bodyError(FORM_TYPE_LOCATION, "Form type is required");
    }

    /**
     * @param rejected the form type code that was not recognised
     * @return an error for an unknown form type, carrying the rejected value
     */
    public static Err formTypeInvalid(final String rejected) {
        return GuardErrors.invalid(ErrorLocation.BODY, FORM_TYPE_LOCATION, "Form type is invalid",
                                        Map.of(REJECTED_KEY, rejected));
    }

    /**
     * @param formType  the recognised form type code that cannot be processed
     * @param formGroup the code of the form group the form belongs to
     * @return an error for a form type whose form group is not supported, carrying the rejected form type and the group
     */
    public static Err formTypeNotSupported(final String formType, final String formGroup) {
        return GuardErrors.invalid(ErrorLocation.BODY, FORM_TYPE_LOCATION,
            "Form " + formType + " not supported because it belongs to the form group " + formGroup,
            Map.of(REJECTED_KEY, formType, FORM_GROUP_KEY, formGroup));
    }

    /**
     * @return an error for a null or empty list of statement types
     */
    public static Err statementTypesMissing() {
        return bodyError(STATEMENT_IDS_LOCATION, "No Statement Types have been supplied");
    }

    /**
     * @param rejected comma-separated distinct invalid codes, with null elements represented as "null"
     * @return an error identifying the invalid statement types
     */
    public static Err statementTypesNotValid(final String rejected) {
        return GuardErrors.invalid(ErrorLocation.BODY, STATEMENT_IDS_LOCATION,
            "One or more of the statements are not valid", Map.of(REJECTED_KEY, rejected));
    }

    /**
     * @param rejected comma-separated distinct duplicated statement codes
     * @return an error identifying the duplicated statement types
     */
    public static Err statementTypesDuplicated(final String rejected) {
        return GuardErrors.invalid(ErrorLocation.BODY, STATEMENT_IDS_LOCATION,
            "Duplicate Statement Types have been supplied", Map.of(REJECTED_KEY, rejected));
    }

    /**
     * @return an error for a null or empty list of data object types
     */
    public static Err dataObjectTypesMissing() {
        return bodyError(DATA_TYPES_LOCATION, "No Data Object Types have been supplied");
    }

    /**
     * @param rejected comma-separated distinct invalid codes, with null elements represented as "null"
     * @return an error identifying the invalid data object types
     */
    public static Err dataObjectTypesNotValid(final String rejected) {
        return GuardErrors.invalid(ErrorLocation.BODY, DATA_TYPES_LOCATION,
            "One or more of the data object types are not valid", Map.of(REJECTED_KEY, rejected));
    }

    /**
     * @param rejected comma-separated distinct duplicated data object type codes
     * @return an error identifying the duplicated data object types
     */
    public static Err dataObjectTypesDuplicated(final String rejected) {
        return GuardErrors.invalid(ErrorLocation.BODY, DATA_TYPES_LOCATION,
            "Duplicate Data Object Types have been supplied", Map.of(REJECTED_KEY, rejected));
    }

    private static Err bodyError(final String location, final String message) {
        return GuardErrors.invalid(ErrorLocation.BODY, location, message);
    }
}

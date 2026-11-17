package uk.gov.companieshouse.presentersapi.error;

public class ErrorMessages {
    static final String DATA_OBJECTS_REQUIRED = "No Data Object Types have been supplied";
    static final String DATA_OBJECTS_NOT_VALID = "One or more of the Data Object Types are not valid";
    static final String DATA_OBJECTS_DUPLICATE = "Duplicate Data Object Types have been supplied";
    static final String FORM_TYPE_REQUIRED = "Form type is required";
    static final String FORM_TYPE_INVALID = "Form type is invalid";
    static final String FORM_TYPE_NOT_SUPPORTED = "Form %s not supported because it belongs to the form group: %s";
    static final String STATEMENT_IDS_REQUIRED = "No Statement Types have been supplied";
    static final String STATEMENT_IDS_NOT_VALID = "One or more of the Statement Types are not valid";
    static final String STATEMENT_IDS_DUPLICATE = "Duplicate Statement Types have been supplied";

    private ErrorMessages() {
    }
}

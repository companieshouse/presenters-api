package uk.gov.companieshouse.presentersapi.exception;

public class FormTypeNotFoundException extends RuntimeException {

    public FormTypeNotFoundException(String message) {
        super(message);
    }
}

package uk.gov.companieshouse.presentersapi.exception;

public class DataObjectTypeNotFoundException extends RuntimeException {

    public DataObjectTypeNotFoundException(String message) {
        super(message);
    }
}

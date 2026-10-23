package uk.gov.companieshouse.presentersapi.exception;

public class PresenterNotFoundException extends RuntimeException {

    public PresenterNotFoundException(String message) {
        super(message);
    }
}

package uk.gov.companieshouse.presentersapi.exception;

/**
 * Exception thrown when a presenter is not verified.
 */
public class PresenterNotVerifiedException extends RuntimeException {

    public PresenterNotVerifiedException(String message) {
        super(message);
    }
}

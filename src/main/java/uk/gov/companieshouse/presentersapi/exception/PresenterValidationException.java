package uk.gov.companieshouse.presentersapi.exception;

/**
 * Exception thrown when input validation fails.
 */
public class PresenterValidationException extends RuntimeException {

    public PresenterValidationException(String message) {
        super(message);
    }
}

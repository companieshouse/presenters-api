package uk.gov.companieshouse.presentersapi.exception;

/**
 * Exception thrown when a presenter type is invalid.
 */
public class PresenterTypeNotValidException extends RuntimeException {

    public PresenterTypeNotValidException(String message) {
        super(message);
    }
}

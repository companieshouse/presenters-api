package uk.gov.companieshouse.presentersapi.exception;

/**
 * Exception thrown when a presenter type is not found.
 */
public class PresenterTypeNotFoundException extends RuntimeException {

    public PresenterTypeNotFoundException(String message) {
        super(message);
    }
}

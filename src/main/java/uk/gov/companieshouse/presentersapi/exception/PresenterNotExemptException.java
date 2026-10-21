package uk.gov.companieshouse.presentersapi.exception;

/**
 * Exception thrown when a presenter is not exempt.
 */
public class PresenterNotExemptException extends RuntimeException {

    public PresenterNotExemptException(String message) {
        super(message);
    }
}

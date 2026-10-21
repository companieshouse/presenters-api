package uk.gov.companieshouse.presentersapi.exception;

public class InternalServerError extends RuntimeException {

    public InternalServerError(String message) {
        super(message);
    }
}

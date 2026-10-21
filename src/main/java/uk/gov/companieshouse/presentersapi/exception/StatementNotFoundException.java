package uk.gov.companieshouse.presentersapi.exception;

public class StatementNotFoundException extends RuntimeException {

    public StatementNotFoundException(String message) {
        super(message);
    }
}

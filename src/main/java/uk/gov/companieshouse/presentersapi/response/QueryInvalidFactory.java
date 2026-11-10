package uk.gov.companieshouse.presentersapi.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.ResponseEntityFactory;

public class QueryInvalidFactory implements ResponseEntityFactory {

    @Override
    public <T> ResponseEntity<ChResponseBody<T>> createResponseEntity(final ServiceResult<T> serviceResult) {
        final ChResponseBody<T> body = ChResponseBody.createErrorsBody(serviceResult.getErrors());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(body);
    }

    @Override
    public ServiceResultStatus getStatusToMatch() {
        return ServiceResultStatus.VALIDATION_ERROR;
    }
}

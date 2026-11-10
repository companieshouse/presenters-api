package uk.gov.companieshouse.presentersapi.response;

import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.response.ChResponseBody;
import uk.gov.companieshouse.service.rest.response.ResponseEntityFactory;

public class QueryFoundFactory implements ResponseEntityFactory {

    @Override
    public <T> ResponseEntity<ChResponseBody<T>> createResponseEntity(final ServiceResult<T> serviceResult) {
        final var body = ChResponseBody.createNormalBody(serviceResult.getData());

        return ResponseEntity.ok().body(body);
    }

    @Override
    public ServiceResultStatus getStatusToMatch() {
        return ServiceResultStatus.FOUND;
    }
}

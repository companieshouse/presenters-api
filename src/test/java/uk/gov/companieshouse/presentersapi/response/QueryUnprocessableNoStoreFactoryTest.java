package uk.gov.companieshouse.presentersapi.response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;

class QueryUnprocessableNoStoreFactoryTest {

    private final QueryUnprocessableNoStoreFactory factory = new QueryUnprocessableNoStoreFactory();

    @Test
    void shouldReturnUnprocessableContentResponseWithErrorsWhenResultIsInvalid() {
        final var errors = new Errors(Err.serviceErrBuilder().withError("Query is unprocessable").build());
        final var serviceResult = ServiceResult.unprocessable(errors);

        final var response = factory.createResponseEntity(serviceResult);

        assertThat(response.getStatusCode(), is(HttpStatus.UNPROCESSABLE_CONTENT));
        assertThat(response.getHeaders().containsHeaderValue(HttpHeaders.CACHE_CONTROL, "no-store"), is(true));
        assertThat(response.getBody(), is(notNullValue()));
        assertThat(response.getBody().getErrorBody(), is(sameInstance(errors)));
    }

    @Test
    void shouldMatchValidationErrorStatus() {
        assertThat(factory.getStatusToMatch(), is(ServiceResultStatus.UNPROCESSABLE));
    }
}

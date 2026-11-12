package uk.gov.companieshouse.presentersapi.response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;

class QueryFoundNoStoreFactoryTest {

    private final QueryFoundNoStoreFactory factory = new QueryFoundNoStoreFactory();

    @Test
    void shouldReturnFoundContentResponseWithErrorsWhenResultIsFound() {
        final var serviceResult = ServiceResult.found(FormType.AP01);

        final var response = factory.createResponseEntity(serviceResult);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().containsHeaderValue(HttpHeaders.CACHE_CONTROL, "no-store"), is(true));
        assertThat(response.getBody(), is(notNullValue()));
        assertThat(response.getBody().getSuccessBody(), is(FormType.AP01));
    }

    @Test
    void shouldMatchValidationErrorStatus() {
        assertThat(factory.getStatusToMatch(), is(ServiceResultStatus.FOUND));
    }
}

package uk.gov.companieshouse.presentersapi.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;

class ResponseEntityFactoryConfigTest {

    private final ResponseEntityFactoryConfig config = new ResponseEntityFactoryConfig();

    @ParameterizedTest
    @MethodSource("noStoreResults")
    void shouldCreateNoStoreResponseForConfiguredResultStatuses(
        final ServiceResult<?> serviceResult,
        final HttpStatus expectedStatus
    ) {
        final var response = config.createResponseFactory().createResponse(serviceResult);

        assertThat(response.getStatusCode(), is(expectedStatus));
        assertThat(response.getHeaders().containsHeaderValue(HttpHeaders.CACHE_CONTROL, "no-store"), is(true));
    }

    private static Stream<Arguments> noStoreResults() {
        final var errors = new Errors(Err.serviceErrBuilder().withError("Query is invalid").build());

        return Stream.of(
            Arguments.of(ServiceResult.found(FormType.AP01), HttpStatus.OK),
            Arguments.of(ServiceResult.invalid(errors), HttpStatus.BAD_REQUEST),
            Arguments.of(ServiceResult.unprocessable(errors), HttpStatus.UNPROCESSABLE_CONTENT)
        );
    }
}

package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.client.InternalApiClientFactory;

class AccountServiceImplTest {

    private static final String USER_ID = "user-123";
    private static final String USER_DETAILS_URI = "/users/%s";

    private final InternalApiClientFactory apiClientFactory = mock(InternalApiClientFactory.class);
    private final Logger logger = mock(Logger.class);
    private final InternalApiClient apiClient = mock(InternalApiClient.class, RETURNS_DEEP_STUBS);

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        when(apiClientFactory.createAccountClient()).thenReturn(apiClient);
        service = new AccountServiceImpl(logger, apiClientFactory);
    }

    @Test
    void getsUserEmailByUserId() throws Exception {
        final User user = new User();
        user.setEmail("user@example.com");
        when(apiClient.privateAccountsUserResourceHandler().getUserDetails(USER_DETAILS_URI.formatted(USER_ID)).execute())
                .thenReturn(new ApiResponse<>(200, Map.of(), user));

        final String email = service.getUserEmailByUserId(USER_ID);

        assertThat(email).isEqualTo("user@example.com");
    }

    @Test
    void mapsClientCreationIoFailuresToBadGateway() {
        final IllegalStateException cause = new IllegalStateException("CHS_INTERNAL_API_KEY not set");
        when(apiClientFactory.createAccountClient()).thenThrow(cause);

        assertResponseStatus(HttpStatus.BAD_GATEWAY, cause);
    }

    @Test
    void propagatesDownstreamApiErrorStatus() throws IOException, URIValidationException {
        final ApiErrorResponseException cause = new ApiErrorResponseException(
                new HttpResponseException.Builder(401, "Unauthorized", new HttpHeaders()));
        when(apiClient.privateAccountsUserResourceHandler().getUserDetails(USER_DETAILS_URI.formatted(USER_ID)).execute())
                .thenThrow(cause);

        assertResponseStatus(HttpStatus.UNAUTHORIZED, cause);
    }

    @Test
    void mapsInvalidDownstreamStatusCodeToBadGateway() throws Exception {
        final int invalidStatusCode = 99;
        final ApiErrorResponseException cause = new ApiErrorResponseException(
                new HttpResponseException.Builder(invalidStatusCode, "Bad", new HttpHeaders()));
        when(apiClient.privateAccountsUserResourceHandler().getUserDetails(USER_DETAILS_URI.formatted(USER_ID)).execute())
                .thenThrow(cause);

        assertResponseStatus(HttpStatus.BAD_GATEWAY, cause);
        verify(logger).errorContext(
                eq(USER_ID),
                eq("Accounts API returned an invalid HTTP status code"),
                any(IllegalArgumentException.class),
                eq(Map.of("user_id", USER_ID, "status_code", invalidStatusCode)));
    }

    @Test
    void propagatesUriValidationFailures() throws Exception {
        final URIValidationException cause = mock(URIValidationException.class);
        when(apiClient.privateAccountsUserResourceHandler().getUserDetails(USER_DETAILS_URI.formatted(USER_ID)).execute())
                .thenThrow(cause);

        assertThatThrownBy(() -> service.getUserEmailByUserId(USER_ID))
                .isSameAs(cause);
    }
    @Test
    void returnsNotFoundWhenEmailIsBlank() throws Exception {
        final User user = new User();
        user.setEmail("   ");
        when(apiClient.privateAccountsUserResourceHandler().getUserDetails(USER_DETAILS_URI.formatted(USER_ID)).execute())
                .thenReturn(new ApiResponse<>(200, Map.of(), user));

        assertThatThrownBy(() -> service.getUserEmailByUserId(USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void assertResponseStatus(final HttpStatus expectedStatus, final Exception cause) {
        assertThatThrownBy(() -> service.getUserEmailByUserId(USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(expectedStatus);
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }
}

package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.acspmanageusersapi.PrivateAcspManageUsersApiResourceHandler;
import uk.gov.companieshouse.api.handler.acspmanageusersapi.request.PrivateAcspManageUsersApiUserAcspMembershipsGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.client.PrivateApiClientFactory;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcspUserServiceImplTest {

    private static final String USER_ID = "user-123";
    private static final String PASSTHROUGH_TOKEN =
            "{\"token_type\":\"Bearer\",\"access_token\":\"oauth2-token\"}";

    @Mock
    private PrivateApiClientFactory apiClientFactory;
    @Mock
    private PrivateAcspManageUsersApiUserAcspMembershipsGet request;
    @Mock
    private InternalApiClient apiClient;
    @Mock
    private PrivateAcspManageUsersApiResourceHandler handler;
    @Mock
    private Logger logger;

    private AcspUserServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(apiClientFactory.create(PASSTHROUGH_TOKEN)).thenReturn(apiClient);
        when(apiClient.privateAcspManageUsersApiResourceHandler()).thenReturn(handler);
        when(handler.getUserAcspMemberships(false)).thenReturn(request);

        service = new AcspUserServiceImpl(apiClientFactory, logger);
    }

    @Test
    void returnsFirstMembership() throws Exception {
        AcspMembership first = new AcspMembership();
        AcspMembershipsList memberships = new AcspMembershipsList();
        memberships.setItems(List.of(first, new AcspMembership()));
        when(request.execute()).thenReturn(new ApiResponse<>(200, Map.of(), memberships));

        assertThat(service.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN))
                .containsSame(first);
    }

    @Test
    void returnsEmptyWhenUserHasNoMemberships() throws Exception {
        AcspMembershipsList memberships = new AcspMembershipsList();
        memberships.setItems(List.of());
        when(request.execute()).thenReturn(new ApiResponse<>(200, Map.of(), memberships));

        assertThat(service.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).isEmpty();
    }

    @Test
    void returnsEmptyWhenResponseHasNoData() throws Exception {
        when(request.execute()).thenReturn(new ApiResponse<>(200, Map.of(), null));

        assertThat(service.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN))
                .isEmpty();
    }

    @Test
    void returnsEmptyWhenMembershipItemsAreNull() throws Exception {
        AcspMembershipsList memberships = new AcspMembershipsList();
        memberships.setItems(null);
        when(request.execute()).thenReturn(new ApiResponse<>(200, Map.of(), memberships));

        assertThat(service.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).isEmpty();
    }

    @Test
    void mapsClientCreationIoFailuresToBadGateway() throws Exception {
        final IOException cause = new IOException("Connection timed out");
        when(apiClientFactory.create(PASSTHROUGH_TOKEN)).thenThrow(cause);

        assertResponseStatus(HttpStatus.BAD_GATEWAY, cause);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500, 503})
    void propagatesDownstreamApiErrorStatus(final int statusCode) throws Exception {
        final ApiErrorResponseException cause = new ApiErrorResponseException(
                new HttpResponseException.Builder(statusCode, "Error", new HttpHeaders()));
        when(request.execute()).thenThrow(cause);

        assertResponseStatus(HttpStatus.valueOf(statusCode), cause);
    }

    @Test
    void mapsUriValidationFailuresToInternalServerError() throws Exception {
        final URIValidationException cause = mock(URIValidationException.class);
        when(request.execute()).thenThrow(cause);

        assertResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }

    @Test
    void logsUserIdContextWhenRetrievalFails() throws Exception {
        final IOException cause = new IOException("Connection reset");
        when(apiClientFactory.create(PASSTHROUGH_TOKEN)).thenThrow(cause);

        assertThatThrownBy(() -> service.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN))
                .isInstanceOf(ResponseStatusException.class);

        verify(logger).errorContext(eq(USER_ID), anyString(), eq(cause), eq(Map.of("user_id", USER_ID)));
    }

    private void assertResponseStatus(final HttpStatus expectedStatus, final Exception cause) {
        assertThatThrownBy(() -> service.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(expectedStatus);
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }
}

package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class AcspUserServiceImplTest {

    private static final String USER_ID = "user-123";
    private static final String PASSTHROUGH_TOKEN =
            "{\"token_type\":\"Bearer\",\"access_token\":\"oauth2-token\"}";

    private PrivateApiClientFactory apiClientFactory;
    private PrivateAcspManageUsersApiUserAcspMembershipsGet request;
    private AcspUserServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        apiClientFactory = mock(PrivateApiClientFactory.class);
        request = mock(PrivateAcspManageUsersApiUserAcspMembershipsGet.class);
        InternalApiClient apiClient = mock(InternalApiClient.class);
        PrivateAcspManageUsersApiResourceHandler handler =
                mock(PrivateAcspManageUsersApiResourceHandler.class);

        when(apiClientFactory.create(PASSTHROUGH_TOKEN)).thenReturn(apiClient);
        when(apiClient.privateAcspManageUsersApiResourceHandler()).thenReturn(handler);
        when(handler.getUserAcspMemberships(false)).thenReturn(request);

        service = new AcspUserServiceImpl(apiClientFactory, mock(Logger.class));
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
    void mapsClientCreationFailuresToInternalServerError() throws Exception {
        IOException cause = new IOException("Connection timed out");
        when(apiClientFactory.create(PASSTHROUGH_TOKEN)).thenThrow(cause);

        assertInternalServerError(cause);
    }

    @Test
    void mapsDownstreamApiErrorsToInternalServerError() throws Exception {
        ApiErrorResponseException cause = new ApiErrorResponseException(
                new HttpResponseException.Builder(401, "Unauthorized", new HttpHeaders()));
        when(request.execute()).thenThrow(cause);

        assertInternalServerError(cause);
    }

    @Test
    void mapsUriValidationFailuresToInternalServerError() throws Exception {
        URIValidationException cause = mock(URIValidationException.class);
        when(request.execute()).thenThrow(cause);

        assertInternalServerError(cause);
    }

    private void assertInternalServerError(Exception cause) {
        assertThatThrownBy(() -> service.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode())
                            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }
}

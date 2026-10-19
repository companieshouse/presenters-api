package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.handler.acspmanageusersapi.PrivateAcspManageUsersApiResourceHandler;
import uk.gov.companieshouse.api.handler.acspmanageusersapi.request.PrivateAcspManageUsersApiUserAcspMembershipsGet;
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
                .isEqualTo(Optional.empty());
    }
}

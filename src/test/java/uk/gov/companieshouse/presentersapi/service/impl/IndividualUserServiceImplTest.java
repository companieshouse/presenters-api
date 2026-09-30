package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.identityverification.PrivateIdentityVerificationResourceHandler;
import uk.gov.companieshouse.api.handler.identityverification.request.PrivateFindIdentityByUserIdGet;
import uk.gov.companieshouse.api.handler.identityverification.request.PrivateFindUvidsByIdentityIdGet;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.identityverification.model.Uvid;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.identityverification.PrivateUvidListApi;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

class IndividualUserServiceImplTest {

    private static final String INTERNAL_API_URL = "http://api.chs.local:4001";
    private static final String PASSTHROUGH_TOKEN =
            "{\"token_type\":\"Bearer\",\"access_token\":\"oauth2-token\"}";

    @Test
    void getsIdentityByUserId() throws Exception {
        InternalApiClient apiClient = mock(InternalApiClient.class);
        PrivateIdentityVerificationResourceHandler handler =
                mock(PrivateIdentityVerificationResourceHandler.class);
        PrivateFindIdentityByUserIdGet request = mock(PrivateFindIdentityByUserIdGet.class);
        Identity expectedIdentity = new Identity();
        ApiResponse<Identity> response = new ApiResponse<>(200, Map.of(), expectedIdentity);

        when(apiClient.privateIdentityVerificationResourceHandler()).thenReturn(handler);
        when(handler.findIdentityByUserId("/verification/identities", "user-123"))
                .thenReturn(request);
        when(request.execute()).thenReturn(response);

        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(() -> ApiSdkManager.getPrivateSDK(PASSTHROUGH_TOKEN))
                    .thenReturn(apiClient);

            IndividualUserServiceImpl service =
                    new IndividualUserServiceImpl(INTERNAL_API_URL, mock(Logger.class));

            Identity identity = service.getIdentityByUserId("user-123", PASSTHROUGH_TOKEN);

            assertThat(identity).isSameAs(expectedIdentity);
            verify(apiClient).setInternalBasePath(INTERNAL_API_URL);
            verify(handler).findIdentityByUserId("/verification/identities", "user-123");
        }
    }

    @Test
    void mapsIdentityTransportFailuresToBadGateway() {
        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(() -> ApiSdkManager.getPrivateSDK(PASSTHROUGH_TOKEN))
                    .thenThrow(new IOException("Connection timed out"));
            IndividualUserServiceImpl service =
                    new IndividualUserServiceImpl(INTERNAL_API_URL, mock(Logger.class));

            assertThatThrownBy(() -> service.getIdentityByUserId("user-123", PASSTHROUGH_TOKEN))
                    .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        }
    }

    @Test
    void preservesIdentityDownstreamHttpStatus() throws Exception {
        InternalApiClient apiClient = mock(InternalApiClient.class);
        PrivateIdentityVerificationResourceHandler handler =
                mock(PrivateIdentityVerificationResourceHandler.class);
        PrivateFindIdentityByUserIdGet request = mock(PrivateFindIdentityByUserIdGet.class);
        ApiErrorResponseException downstreamException = new ApiErrorResponseException(
                new HttpResponseException.Builder(401, "Unauthorized", new HttpHeaders()));

        when(apiClient.privateIdentityVerificationResourceHandler()).thenReturn(handler);
        when(handler.findIdentityByUserId("/verification/identities", "user-123"))
                .thenReturn(request);
        when(request.execute()).thenThrow(downstreamException);

        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(() -> ApiSdkManager.getPrivateSDK(PASSTHROUGH_TOKEN))
                    .thenReturn(apiClient);
            IndividualUserServiceImpl service =
                    new IndividualUserServiceImpl(INTERNAL_API_URL, mock(Logger.class));

            assertThatThrownBy(() -> service.getIdentityByUserId("user-123", PASSTHROUGH_TOKEN))
                    .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }

    @Test
    void getsTheActiveUvidByIdentityId() throws Exception {
        InternalApiClient apiClient = mock(InternalApiClient.class);
        PrivateIdentityVerificationResourceHandler handler =
                mock(PrivateIdentityVerificationResourceHandler.class);
        PrivateFindUvidsByIdentityIdGet request = mock(PrivateFindUvidsByIdentityIdGet.class);

        Uvid expectedUvid = new Uvid();
        expectedUvid.setUvid("11111-111");
        ApiResponse<PrivateUvidListApi> response = new ApiResponse<>(
                200, Map.of(), new PrivateUvidListApi(List.of(expectedUvid)));

        when(apiClient.privateIdentityVerificationResourceHandler()).thenReturn(handler);
        when(handler.findUvidsByIdentityId(
                "/verification/identities/identity-123/uvids", true)).thenReturn(request);
        when(request.execute()).thenReturn(response);

        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(() -> ApiSdkManager.getPrivateSDK(PASSTHROUGH_TOKEN))
                    .thenReturn(apiClient);

            IndividualUserServiceImpl service =
                    new IndividualUserServiceImpl(INTERNAL_API_URL, mock(Logger.class));

            Uvid uvid = service.getActiveUvidByIdentityId(
                    "identity-123", PASSTHROUGH_TOKEN);

            assertThat(uvid).isSameAs(expectedUvid);
            verify(apiClient).setInternalBasePath(INTERNAL_API_URL);
            verify(handler).findUvidsByIdentityId(
                    "/verification/identities/identity-123/uvids", true);
        }
    }

    @Test
    void mapsTransportFailuresToBadGateway() {
        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(() -> ApiSdkManager.getPrivateSDK(PASSTHROUGH_TOKEN))
                    .thenThrow(new IOException("Connection timed out"));
            IndividualUserServiceImpl service =
                    new IndividualUserServiceImpl(INTERNAL_API_URL, mock(Logger.class));

            assertThatThrownBy(() -> service.getActiveUvidByIdentityId(
                    "identity-123", PASSTHROUGH_TOKEN))
                    .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        }
    }

    @Test
    void preservesDownstreamHttpStatus() throws Exception {
        InternalApiClient apiClient = mock(InternalApiClient.class);
        PrivateIdentityVerificationResourceHandler handler =
                mock(PrivateIdentityVerificationResourceHandler.class);
        PrivateFindUvidsByIdentityIdGet request = mock(PrivateFindUvidsByIdentityIdGet.class);
        ApiErrorResponseException downstreamException = new ApiErrorResponseException(
                new HttpResponseException.Builder(401, "Unauthorized", new HttpHeaders()));

        when(apiClient.privateIdentityVerificationResourceHandler()).thenReturn(handler);
        when(handler.findUvidsByIdentityId(
                "/verification/identities/identity-123/uvids", true)).thenReturn(request);
        when(request.execute()).thenThrow(downstreamException);

        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(() -> ApiSdkManager.getPrivateSDK(PASSTHROUGH_TOKEN))
                    .thenReturn(apiClient);
            IndividualUserServiceImpl service =
                    new IndividualUserServiceImpl(INTERNAL_API_URL, mock(Logger.class));

            assertThatThrownBy(() -> service.getActiveUvidByIdentityId(
                    "identity-123", PASSTHROUGH_TOKEN))
                    .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }
}

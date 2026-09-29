package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.handler.identityverification.PrivateIdentityVerificationResourceHandler;
import uk.gov.companieshouse.api.handler.identityverification.request.PrivateFindUvidsByIdentityIdGet;
import uk.gov.companieshouse.api.identityverification.model.Uvid;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.identityverification.PrivateUvidListApi;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

class IdentityVerificationServiceImplTest {

    private static final String INTERNAL_API_URL = "http://api.chs.local:4001";
    private static final String PASSTHROUGH_TOKEN =
            "{\"token_type\":\"Bearer\",\"access_token\":\"oauth2-token\"}";

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

            IdentityVerificationServiceImpl service =
                    new IdentityVerificationServiceImpl(INTERNAL_API_URL, mock(Logger.class));

            Uvid uvid = service.getActiveUvidByIdentityId(
                    "identity-123", PASSTHROUGH_TOKEN);

            assertThat(uvid).isSameAs(expectedUvid);
            verify(apiClient).setInternalBasePath(INTERNAL_API_URL);
            verify(handler).findUvidsByIdentityId(
                    "/verification/identities/identity-123/uvids", true);
        }
    }
}

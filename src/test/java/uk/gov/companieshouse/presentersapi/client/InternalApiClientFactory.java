package uk.gov.companieshouse.presentersapi.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

class InternalApiClientFactoryTest {

    private static final String INTERNAL_API_URL = "http://api.chs.local:4001";
    private static final String PASSTHROUGH_TOKEN =
            "{\"token_type\":\"Bearer\",\"access_token\":\"oauth2-token\"}";

    @Test
    void createsClientWithPassthroughTokenAndInternalBasePath() throws Exception {
        InternalApiClient apiClient = mock(InternalApiClient.class);

        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(() -> ApiSdkManager.getPrivateSDK(PASSTHROUGH_TOKEN))
                    .thenReturn(apiClient);

            InternalApiClient result =
                    new InternalApiClientFactory(INTERNAL_API_URL).create(PASSTHROUGH_TOKEN);

            assertThat(result).isSameAs(apiClient);
            verify(apiClient).setInternalBasePath(INTERNAL_API_URL);
        }
    }

    @Test
    void propagatesSdkFailures() {
        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(() -> ApiSdkManager.getPrivateSDK(PASSTHROUGH_TOKEN))
                    .thenThrow(new IOException("Connection timed out"));

            assertThatThrownBy(() ->
                    new InternalApiClientFactory(INTERNAL_API_URL).create(PASSTHROUGH_TOKEN))
                    .isInstanceOf(IOException.class);
        }
    }

    @Test
    void createsInternalClientWithInternalBasePath() {
        InternalApiClient apiClient = mock(InternalApiClient.class);

        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(ApiSdkManager::getInternalSDK)
                    .thenReturn(apiClient);

            InternalApiClient result =
                    new InternalApiClientFactory(INTERNAL_API_URL).create();

            assertThat(result).isSameAs(apiClient);
            verify(apiClient).setInternalBasePath(INTERNAL_API_URL);
        }
    }

    @Test
    void propagatesInternalSdkFailures() {
        try (MockedStatic<ApiSdkManager> sdkManager = mockStatic(ApiSdkManager.class)) {
            sdkManager.when(ApiSdkManager::getInternalSDK)
                    .thenThrow(new IllegalStateException("CHS_INTERNAL_API_KEY not set"));
            final InternalApiClientFactory factory = new InternalApiClientFactory(INTERNAL_API_URL);

            assertThatThrownBy(factory::create)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CHS_INTERNAL_API_KEY");
        }
    }
}

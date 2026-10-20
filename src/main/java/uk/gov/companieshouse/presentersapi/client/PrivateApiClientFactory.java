package uk.gov.companieshouse.presentersapi.client;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

/**
 * Creates private API SDK clients that act on behalf of the calling user by forwarding the
 * passthrough token supplied by ERIC.
 */
@Component
public class PrivateApiClientFactory {

    private final String internalApiUrl;

    public PrivateApiClientFactory(@Value("${internal.api.url}") final String internalApiUrl) {
        this.internalApiUrl = internalApiUrl;
    }

    public InternalApiClient create(final String passthroughToken) throws IOException {
        final InternalApiClient apiClient = ApiSdkManager.getPrivateSDK(passthroughToken);
        // ApiSdkManager only sets the public base path; private handlers use this path.
        apiClient.setInternalBasePath(internalApiUrl);
        return apiClient;
    }
}

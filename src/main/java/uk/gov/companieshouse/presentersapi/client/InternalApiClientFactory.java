package uk.gov.companieshouse.presentersapi.client;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

/**
 * Creates {@link InternalApiClient} instances for both supported authentication modes.
 *
 * <p>This factory intentionally overloads {@code create(...)} so callers can select the correct
 * downstream authentication behaviour for each API:</p>
 * <ul>
 *     <li>{@link #create(String)}: uses ERIC OAuth passthrough context via
 *     {@link ApiSdkManager#getPrivateSDK(String)} for APIs that authorise user-context calls.</li>
 *     <li>{@link #create()}: uses internal API-key context via
 *     {@link ApiSdkManager#getInternalSDK()} for APIs that require internal-app authentication
 *     rather than OAuth passthrough.</li>
 * </ul>
 */
@Component
public class InternalApiClientFactory {

    private final String internalApiUrl;

    public InternalApiClientFactory(@Value("${internal.api.url}") final String internalApiUrl) {
        this.internalApiUrl = internalApiUrl;
    }

    /**
     * Creates a client authenticated with the provided ERIC passthrough token.
     *
     * <p>Use this overload for downstream APIs that accept OAuth user-context authorisation.</p>
     *
     * @param passthroughToken JSON ERIC passthrough token header value
     * @return configured internal API client
     * @throws IOException if the passthrough token cannot be parsed by the SDK
     */
    public InternalApiClient create(final String passthroughToken) throws IOException {
        final InternalApiClient apiClient = ApiSdkManager.getPrivateSDK(passthroughToken);
        // ApiSdkManager only sets the public base path; private handlers use this path.
        apiClient.setInternalBasePath(internalApiUrl);
        return apiClient;
    }

    /**
     * Creates a client authenticated as an internal app using {@code CHS_INTERNAL_API_KEY}.
     *
     * <p>Use this overload for downstream APIs/endpoints that require internal-api-key
     * authentication and reject OAuth passthrough identity types.</p>
     *
     * @return configured internal API client
     */
    public InternalApiClient create() {
        final InternalApiClient apiClient = ApiSdkManager.getInternalSDK();
        // ApiSdkManager only sets the public base path; private handlers use this path.
        apiClient.setInternalBasePath(internalApiUrl);
        return apiClient;
    }
}

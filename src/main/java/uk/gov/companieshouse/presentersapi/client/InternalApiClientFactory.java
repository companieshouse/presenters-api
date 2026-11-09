package uk.gov.companieshouse.presentersapi.client;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

/**
 * Creates {@link InternalApiClient} instances for both supported authentication modes.
 *
 * <p>Callers can select the correct downstream authentication behaviour and base URL for each API:</p>
 * <ul>
 *     <li>{@link #create(String)}: uses ERIC OAuth passthrough context via
 *     {@link ApiSdkManager#getPrivateSDK(String)} for APIs that authorise user-context calls.</li>
 *     <li>{@link #create()}: uses internal API-key context via
 *     {@link ApiSdkManager#getInternalSDK()} for APIs that require internal-app authentication
 *     rather than OAuth passthrough.</li>
 *     <li>{@link #createAccountClient()}: uses internal API-key context via
 *     {@link ApiSdkManager#getInternalSDK()} with the configured Account API URL for account
 *     user lookups.</li>
 * </ul>
 */
@Component
public class InternalApiClientFactory {

    private final String internalApiUrl;
    private final String accountsApiUrl;

    public InternalApiClientFactory(@Value("${internal.api.url}") final String internalApiUrl,
            @Value("${accounts.api.url}") final String accountsApiUrl) {
        this.internalApiUrl = internalApiUrl;
        this.accountsApiUrl = accountsApiUrl;
    }

    /**
     * Creates a private API client that acts on behalf of the calling user by forwarding the
     * supplied passthrough token. The client's base path is set to the configured internal API URL.
     *
     * @param passthroughToken the passthrough token supplied by ERIC for the calling user
     * @return an {@link InternalApiClient} authorised with the passthrough token
     * @throws IOException if the SDK client cannot be created
     */
    public InternalApiClient create(final String passthroughToken) throws IOException {
        final InternalApiClient apiClient = ApiSdkManager.getPrivateSDK(passthroughToken);
        // ApiSdkManager only sets the public base path; private handlers use this path.
        apiClient.setInternalBasePath(internalApiUrl);
        return apiClient;
    }

    /**
     * Creates an internal API client that authenticates as the service itself, rather than on behalf
     * of a user. The client's base path is set to the configured internal API URL.
     *
     * @return an {@link InternalApiClient} using the service's own credentials
     */
    public InternalApiClient create() {
        final InternalApiClient apiClient = ApiSdkManager.getInternalSDK();
        // ApiSdkManager only sets the public base path; private handlers use this path.
        apiClient.setInternalBasePath(internalApiUrl);
        return apiClient;
    }

    /**
     * Creates an internal API client using the service's own credentials and the Accounts API URL.
     *
     * @return an {@link InternalApiClient} configured for Accounts API requests
     */
    public InternalApiClient createAccountClient() {
        final var internalApiClient = ApiSdkManager.getInternalSDK();
        internalApiClient.setInternalBasePath(accountsApiUrl);
        return internalApiClient;
    }
}

package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.service.IdentityVerificationService;

@ExtendWith(MockitoExtension.class)
class IndividualUserServiceImplTest {

    private static final String USER_ID = "user-123";
    private static final String PASSTHROUGH_TOKEN =
            "{\"token_type\":\"Bearer\",\"access_token\":\"oauth2-token\"}";

    @Mock
    private HttpServletRequest request;

    @Mock
    private IdentityVerificationService identityVerificationService;
    @Mock
    private Logger logger;

    private IndividualUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new IndividualUserServiceImpl(identityVerificationService, logger);
    }

    @Test
    void getsIdentityVerificationDetailsForTheSignedInUser() throws URIValidationException {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn("oauth2");
        when(request.getHeader(ERIC_IDENTITY)).thenReturn(USER_ID);
        when(request.getHeader("ERIC-Access-Token")).thenReturn(PASSTHROUGH_TOKEN);

        Identity expectedIdentity = new Identity();
        expectedIdentity.setId("identity-123");
        expectedIdentity.setEmail("presenter@example.com");
        expectedIdentity.setUserId(USER_ID);
        when(identityVerificationService.getIdentityByUserId(USER_ID, PASSTHROUGH_TOKEN))
                .thenReturn(expectedIdentity);

        Identity identity = service.getIdentityVerificationDetails(request);

        assertThat(identity.getId()).isEqualTo("identity-123");
        assertThat(identity.getEmail()).isEqualTo("presenter@example.com");
        assertThat(identity.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void rejectsNonOAuth2Requests() {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn("key");

        assertThatThrownBy(() -> service.getIdentityVerificationDetails(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("An OAuth2 user is required");
    }

    @Test
    void rejectsRequestsMissingTheAccessTokenHeader() {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn("oauth2");
        when(request.getHeader(ERIC_IDENTITY)).thenReturn(USER_ID);
        when(request.getHeader("ERIC-Access-Token")).thenReturn(null);

        assertThatThrownBy(() -> service.getIdentityVerificationDetails(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ERIC-Access-Token");
    }
}

package uk.gov.companieshouse.presentersapi.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserHelperTest {

    @Mock
    private HttpServletRequest request;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getsChsUserIdForOauth2User() {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn("oauth2");
        when(request.getHeader(ERIC_IDENTITY)).thenReturn("user-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(UserHelper.getChsUserId()).isEqualTo("user-123");
    }

    @Test
    void rejectsNonOauth2User() {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn("apikey");
        when(request.getHeader(ERIC_IDENTITY)).thenReturn("user-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(UserHelper::getChsUserId)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("An OAuth2 user is required");
    }

    @Test
    void rejectsBlankUserId() {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn("oauth2");
        when(request.getHeader(ERIC_IDENTITY)).thenReturn(" ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(UserHelper::getChsUserId)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("An OAuth2 user is required");
    }

    @Test
    void rejectsChsUserIdLookupWhenNoRequestIsBound() {
        assertThatThrownBy(UserHelper::getChsUserId)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiresNonBlankHeader() {
        when(request.getHeader("ERIC-Access-Token")).thenReturn("token");

        assertThat(UserHelper.requireHeader(request, "ERIC-Access-Token")).isEqualTo("token");
    }

    @Test
    void rejectsMissingHeader() {
        when(request.getHeader("ERIC-Access-Token")).thenReturn(null);

        assertThatThrownBy(() -> UserHelper.requireHeader(request, "ERIC-Access-Token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing required header: ERIC-Access-Token");
    }

    @Test
    void getsPassthroughTokenFromCurrentRequest() {
        when(request.getHeader("ERIC-Access-Token")).thenReturn("token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(UserHelper.requirePassthroughToken()).isEqualTo("token");
    }

    @Test
    void rejectsMissingPassthroughTokenOnCurrentRequest() {
        when(request.getHeader("ERIC-Access-Token")).thenReturn(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(UserHelper::requirePassthroughToken)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing required header: ERIC-Access-Token");
    }

    @Test
    void rejectsPassthroughTokenLookupWhenNoRequestIsBound() {
        assertThatThrownBy(UserHelper::requirePassthroughToken)
                .isInstanceOf(IllegalStateException.class);
    }
}

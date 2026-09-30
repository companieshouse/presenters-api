package uk.gov.companieshouse.presentersapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.identityverification.model.CurrentName;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.identityverification.model.Uvid;
import uk.gov.companieshouse.presentersapi.model.IndividualUserResponse;
import uk.gov.companieshouse.presentersapi.service.IdentityVerificationService;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;

@ExtendWith(MockitoExtension.class)
class IndividualUserTestControllerTest {

    private static final String PASSTHROUGH_TOKEN =
            "{\"token_type\":\"Bearer\",\"access_token\":\"oauth2-token\"}";

    @Mock
    private IndividualUserService individualUserService;
    @Mock
    private IdentityVerificationService identityVerificationService;
    @Mock
    private HttpServletRequest request;

    private IndividualUserTestController controller;

    @BeforeEach
    void setUp() {
        controller = new IndividualUserTestController(
                individualUserService, identityVerificationService);
    }

    @Test
    void returnsIndividualUserDetails() throws Exception {
        CurrentName currentName = new CurrentName();
        currentName.setForenames(List.of("Test"));
        currentName.setSurname("User");

        Identity identity = new Identity();
        identity.setId("identity-123");
        identity.setCurrentName(currentName);
        identity.setDateOfBirth(LocalDate.of(1970, 1, 1));
        identity.setEmail("test@b.com");

        Uvid uvid = new Uvid();
        uvid.setUvid("11111-111");

        when(request.getHeader("ERIC-Access-Token")).thenReturn(PASSTHROUGH_TOKEN);
        when(individualUserService.getIdentityVerificationDetails(request)).thenReturn(identity);
        when(identityVerificationService.getActiveUvidByIdentityId(
                "identity-123", PASSTHROUGH_TOKEN)).thenReturn(uvid);

        IndividualUserResponse response = controller.getIdentityVerificationDetails(request);

        assertThat(response.individualUser().uvid()).isEqualTo("11111-111");
        assertThat(response.individualUser().name()).isEqualTo("Test User");
        assertThat(response.individualUser().dob()).isEqualTo("01/01/1970");
        assertThat(response.individualUser().email()).isEqualTo("test@b.com");
        assertThat(JsonMapper.builder().build().writeValueAsString(response))
                .isEqualTo("{\"individual_user\":{\"UVID\":\"11111-111\","
                        + "\"name\":\"Test User\",\"dob\":\"01/01/1970\","
                        + "\"email\":\"test@b.com\"}}");
        verify(identityVerificationService)
                .getActiveUvidByIdentityId("identity-123", PASSTHROUGH_TOKEN);
    }
}

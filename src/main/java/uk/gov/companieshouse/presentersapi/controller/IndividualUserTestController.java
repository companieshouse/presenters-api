package uk.gov.companieshouse.presentersapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.CurrentName;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.identityverification.model.Uvid;
import uk.gov.companieshouse.presentersapi.model.IndividualUserResponse;
import uk.gov.companieshouse.presentersapi.model.IndividualUserResponse.IndividualUser;
import uk.gov.companieshouse.presentersapi.service.IdentityVerificationService;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;

/**
 * Temporary controller for manual end-to-end testing of {@link IndividualUserService}
 * in Docker. Remove once the real presenters-api endpoints exist.
 */
@RestController
public class IndividualUserTestController {

    private static final String ERIC_ACCESS_TOKEN_HEADER = "ERIC-Access-Token";
    private static final DateTimeFormatter DATE_OF_BIRTH_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IndividualUserService individualUserService;
    private final IdentityVerificationService identityVerificationService;

    public IndividualUserTestController(
            IndividualUserService individualUserService,
            IdentityVerificationService identityVerificationService) {
        this.individualUserService = individualUserService;
        this.identityVerificationService = identityVerificationService;
    }

    @GetMapping("/presenters/test/individual-user")
    public IndividualUserResponse getIdentityVerificationDetails(HttpServletRequest request)
            throws URIValidationException {
        Identity identity = individualUserService.getIdentityVerificationDetails(request);
        validateIdentityDetails(identity);
        Uvid uvid = identityVerificationService.getActiveUvidByIdentityId(
                identity.getId(), requirePassthroughToken(request));

        return new IndividualUserResponse(new IndividualUser(
                uvid.getUvid(),
                formatName(identity.getCurrentName()),
                DATE_OF_BIRTH_FORMAT.format(identity.getDateOfBirth()),
                identity.getEmail()));
    }

    private String requirePassthroughToken(HttpServletRequest request) {
        String passthroughToken = request.getHeader(ERIC_ACCESS_TOKEN_HEADER);
        if (passthroughToken == null || passthroughToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing required header: " + ERIC_ACCESS_TOKEN_HEADER);
        }
        return passthroughToken;
    }

    private void validateIdentityDetails(Identity identity) {
        CurrentName currentName = identity.getCurrentName();
        if (identity.getId() == null || identity.getId().isBlank()
                || identity.getEmail() == null || identity.getEmail().isBlank()
                || identity.getDateOfBirth() == null
                || currentName == null
                || currentName.getForenames() == null
                || currentName.getForenames().isEmpty()
                || currentName.getSurname() == null
                || currentName.getSurname().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Identity-verification returned incomplete individual details");
        }
    }

    private String formatName(CurrentName currentName) {
        List<String> nameParts = new ArrayList<>(currentName.getForenames());
        nameParts.add(currentName.getSurname());
        return String.join(" ", nameParts);
    }
}

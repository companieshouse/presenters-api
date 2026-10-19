package uk.gov.companieshouse.presentersapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.utils.UserHelper;

@RestController
public class AcspUserController {

    private final AcspUserService acspUserService;

    public AcspUserController(final AcspUserService acspUserService) {
        this.acspUserService = acspUserService;
    }

    @GetMapping("/presenters/test/acsp-membership")
    public ResponseEntity<AcspMembership> getAcspMembership(final HttpServletRequest request)
            throws URIValidationException {
        final String userId = UserHelper.getChsUserId(request);
        final String passthroughToken = UserHelper.requirePassthroughToken(request);

        return acspUserService.getAcspMembershipForUser(userId, passthroughToken)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User is not an ACSP member"));
    }
}

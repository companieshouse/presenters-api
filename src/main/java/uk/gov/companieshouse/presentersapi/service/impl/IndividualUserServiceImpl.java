package uk.gov.companieshouse.presentersapi.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.service.IdentityVerificationService;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;

@Service
public class IndividualUserServiceImpl implements IndividualUserService {

    private static final String ERIC_ACCESS_TOKEN_HEADER = "ERIC-Access-Token";

    private final IdentityVerificationService identityVerificationService;
    private final Logger logger;

    public IndividualUserServiceImpl(
            IdentityVerificationService identityVerificationService,
            Logger logger) {
        this.identityVerificationService = identityVerificationService;
        this.logger = logger;
    }

    @Override
    public Identity getIdentityVerificationDetails(HttpServletRequest request) throws URIValidationException {
        // Eric has validated the OAuth2 token before forwarding the request, so
        // ERIC-Identity contains the resolved CHS user ID and so
        // no need to call authentication-service
        String userId = getChsUserId(request);
        String passthroughToken = requireHeader(request);

        logger.debugContext(userId, "Retrieving identity details for signed-in user",
                Map.of("user_id", userId));
        Identity identity = identityVerificationService.getIdentityByUserId(userId, passthroughToken);
        logger.infoContext(userId, "Retrieved identity details for signed-in user",
                Map.of("user_id", userId));
        return identity;
    }

    private String getChsUserId(HttpServletRequest request) {
        String userId = AuthorisationUtil.getAuthorisedIdentity(request);

        if (!AuthorisationUtil.isOauth2User(request) || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "An OAuth2 user is required");
        }

        return userId;
    }

    private String requireHeader(HttpServletRequest request) {
        String value = request.getHeader(IndividualUserServiceImpl.ERIC_ACCESS_TOKEN_HEADER);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing required header: " + IndividualUserServiceImpl.ERIC_ACCESS_TOKEN_HEADER);
        }

        return value;
    }
}

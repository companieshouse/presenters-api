package uk.gov.companieshouse.presentersapi.service;

import java.util.Optional;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

/**
 * Retrieves ACSP membership details from the acsp-manage-users-api through the private API SDK.
 */
public interface AcspUserService {

    /**
     * @param userId           the CHS user ID, used for logging
     * @param passthroughToken the ERIC passthrough token identifying the user to the downstream API
     * @return the user's ACSP membership, or empty if the user is not an ACSP member
     * @throws org.springframework.web.server.ResponseStatusException with status 500 if the memberships cannot be
     *     retrieved
     */
    Optional<AcspMembership> getAcspMembershipForUser(String userId, String passthroughToken);
}

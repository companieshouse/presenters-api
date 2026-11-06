package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.EnumSet;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.AccountService;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@Service
public class PresenterTypesServiceImpl implements PresenterTypesService {

    private final PresenterTypeMatrixService presenterTypeMatrixService;
    private final AcspUserService acspUserService;
    private final ExemptUserService exemptUserService;
    private final AccountService accountService;
    private final IndividualUserService individualUserService;
    private final Logger logger;

    private static final String USER_ID = "user_id";

    // Create a list of Exempt User presenter types for more flexibility
    private static final EnumSet<PresenterType> EXEMPT_USER_PRESENTER_TYPES = EnumSet.of(PresenterType.OFFICIAL_RECEIVER);

    public PresenterTypesServiceImpl(final PresenterTypeMatrixService presenterTypeMatrixService,
									 final AcspUserService acspUserService, final ExemptUserService exemptUserService, AccountService accountService,
                                     final IndividualUserService individualUserService, final Logger logger) {
        this.presenterTypeMatrixService = presenterTypeMatrixService;
        this.acspUserService = acspUserService;
        this.exemptUserService = exemptUserService;
		this.accountService = accountService;
        this.individualUserService = individualUserService;
        this.logger = logger;
    }

    @Override
    public EnumSet<PresenterType> getPresenterTypes(final String userId, final String formType,
        final String passthroughToken) {

        final var formTypeEnum = toFormType(formType);
        final var presenterTypes = EnumSet.copyOf(presenterTypeMatrixService.presenterTypesFor(formTypeEnum));

        final Map<String, Object> logMap = Map.of(
            USER_ID, userId, "form_type", formType, "presenter_types", presenterTypes.toString());
        logger.debugContext(userId, "Retrieved possible presenter types for form type", logMap);

        // Call out to ExemptUser service
        if (isExemptUser(userId)) {
            presenterTypes.addAll(EXEMPT_USER_PRESENTER_TYPES);
        }

        // Only call out to the ACSP service if the presenter types include ACSP for this form type
        if (presenterTypes.contains(PresenterType.ACSP) && !isValidAcsp(userId, passthroughToken)) {
            presenterTypes.remove(PresenterType.ACSP);
        }

        // Check verification only if individual presenter types are present; retain only ACSP and official receiver if unverified.
        if (presenterTypes.stream().anyMatch(presenterType ->
            presenterType != PresenterType.OFFICIAL_RECEIVER && presenterType != PresenterType.ACSP)
            && !isVerifiedIndividual(userId)) {
            presenterTypes.retainAll(EnumSet.of(PresenterType.ACSP, PresenterType.OFFICIAL_RECEIVER));
        }

        logger.debugContext(userId, "Resolved possible presenter types for form type and user", logMap);

        return presenterTypes;
    }

    private static FormType toFormType(final String formType) {
        return FormType.from(formType);
    }

    private boolean isValidAcsp(final String userId, final String passthroughToken) {
        return acspUserService.getAcspMembershipForUser(userId, passthroughToken)
            .filter(PresenterTypesServiceImpl::isActiveMembershipOfActiveAcsp)
            .isPresent();
    }

    private static boolean isActiveMembershipOfActiveAcsp(final AcspMembership membership) {
        return membership.getMembershipStatus() == MembershipStatusEnum.ACTIVE
            && membership.getAcspStatus() == AcspStatusEnum.ACTIVE;
    }

    private boolean isExemptUser(final String userId) {
        final Map<String, Object> logMap = Map.of(USER_ID, userId);
        try {
            final String email = accountService.getUserEmailByUserId(userId);
            return exemptUserService.isExemptUserByEmail(email);
        } catch (final ResponseStatusException | URIValidationException exception) {
            logger.errorContext(userId, "Failed to resolve exempt user status; defaulting to non-exempt",
                    exception, logMap);
            return false;
        }
    }

    private boolean isVerifiedIndividual(final String userId) {
        try {
            final var identity = individualUserService.getIdentityByUserId(userId);
            individualUserService.getActiveUvidByIdentityId(identity.getId());
            return true;
        } catch (final ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                logger.debugContext(userId, "No verified identity or active UVID found for user", Map.of(USER_ID, userId));
                return false;
            }
            throw exception;
        } catch (final URIValidationException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid identity-verification URI", exception);
        }
    }

}

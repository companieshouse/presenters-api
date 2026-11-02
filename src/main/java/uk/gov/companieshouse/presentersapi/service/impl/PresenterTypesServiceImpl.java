package uk.gov.companieshouse.presentersapi.service.impl;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.service.AccountService;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;
import uk.gov.companieshouse.presentersapi.utils.RequestHelper;

@Service
public class PresenterTypesServiceImpl implements PresenterTypesService {

    private final PresenterTypeMatrixService presenterTypeMatrixService;
    private final AcspUserService acspUserService;
    private final ExemptUserService exemptUserService;
    private final AccountService accountService;
    private final Logger logger;

    // Create a list of Exempt User presenter types for more flexibility
    private static final EnumSet<PresenterType> EXEMPT_USER_PRESENTER_TYPES = EnumSet.of(PresenterType.OFFICIAL_RECEIVER);

    public PresenterTypesServiceImpl(final PresenterTypeMatrixService presenterTypeMatrixService,
									 final AcspUserService acspUserService, final ExemptUserService exemptUserService, AccountService accountService,
									 final Logger logger) {
        this.presenterTypeMatrixService = presenterTypeMatrixService;
        this.acspUserService = acspUserService;
        this.exemptUserService = exemptUserService;
		this.accountService = accountService;
		this.logger = logger;
    }

    @Override
    public EnumSet<PresenterType> getPresenterTypes(final String userId, final String formType,
        final String passthroughToken) {

        final var formTypeEnum = toFormType(formType);
        final var presenterTypes = EnumSet.copyOf(presenterTypeMatrixService.presenterTypesFor(formTypeEnum));
        final String oauthUserId = RequestHelper.getChsUserId();
        final Map<String, Object> logMap = Map.of(
            "user_id", userId, "form_type", formType, "presenter_types", presenterTypes.toString());
        logger.debugContext(userId, "Retrieved possible presenter types for form type", logMap);
        // Call out to ExemptUser service

        if (isExemptUser(oauthUserId, passthroughToken)) {
            presenterTypes.addAll(EXEMPT_USER_PRESENTER_TYPES);
        }
        // Only call out to the ACSP service if the presenter types include ACSP for this form type
        if (presenterTypes.contains(PresenterType.ACSP) && !isValidAcsp(userId, passthroughToken)) {
            presenterTypes.remove(PresenterType.ACSP);
        }

        logger.debugContext(userId, "Resolved possible presenter types for form type and user", logMap);

        return presenterTypes;
    }

    private static FormType toFormType(final String formType) {
        try {
            return FormType.from(formType);
        } catch (final IllegalArgumentException exception) {
            throw new FormTypeNotFoundException("Form Type: %s".formatted(formType));
        }
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

    private boolean isExemptUser(final String userId, final String passthroughToken) {
        final Map<String, Object> logMap = Map.of("user_id", userId);
        try {
            final String email = accountService.getUserEmailByUserId(userId, passthroughToken);
            return exemptUserService.isExemptUserByEmail(email);
        } catch (final ResponseStatusException | IOException | URIValidationException exception) {
            logger.errorContext(userId, "Failed to resolve exempt user status; defaulting to non-exempt",
                    exception, logMap);
            return false;
        }
    }
}

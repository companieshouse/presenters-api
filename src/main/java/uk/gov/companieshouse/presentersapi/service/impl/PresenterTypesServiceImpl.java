package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.Collections;
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
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.guard.FormTypeGuard;
import uk.gov.companieshouse.presentersapi.guard.Guarded;
import uk.gov.companieshouse.presentersapi.service.AccountService;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;

@Service
public class PresenterTypesServiceImpl implements PresenterTypesService {

    private final PresenterTypeMatrixService presenterTypeMatrixService;
    private final FormTypeGuard formTypeGuard;
    private final AcspUserService acspUserService;
    private final ExemptUserService exemptUserService;
    private final AccountService accountService;
    private final Logger logger;

    private static final String USER_ID = "user_id";
    private static final String FORM_TYPE = "form_type";

    // Create a list of Exempt User presenter types for more flexibility
    private static final EnumSet<PresenterType> EXEMPT_USER_PRESENTER_TYPES = EnumSet.of(PresenterType.OFFICIAL_RECEIVER);

    public PresenterTypesServiceImpl(final PresenterTypeMatrixService presenterTypeMatrixService,
                                     final AcspUserService acspUserService, final ExemptUserService exemptUserService, final AccountService accountService,
                                     final Logger logger, final FormTypeGuard formTypeGuard) {
        this.presenterTypeMatrixService = presenterTypeMatrixService;
        this.formTypeGuard = formTypeGuard;
        this.acspUserService = acspUserService;
        this.exemptUserService = exemptUserService;
        this.accountService = accountService;
        this.logger = logger;
    }

    @Override
    public ServiceResult<EnumSet<PresenterType>> getPresenterTypes(final String userId, final String formType,
        final String passthroughToken) {

        Errors errors = new Errors();

        final var formTypeEnum = formTypeGuard.fromQuery(formType);
        if (!formTypeEnum.isValid()) {
            return ServiceResult.invalid(Guarded.combinedErrors(formTypeEnum));
        }
        final var presenterTypes = EnumSet.copyOf(
            presenterTypeMatrixService.presenterTypesAllowedFor(formTypeEnum.value()));

        final Map<String, Object> logMap = Map.of(
            USER_ID, userId, FORM_TYPE, formType, "presenter_types", presenterTypes.toString());
        logger.debugContext(userId, "Retrieved possible presenter types for form type", logMap);

        // Only call out to the ACSP service if the presenter types include ACSP for this form type
        if (presenterTypes.contains(PresenterType.ACSP) && !isValidAcsp(userId, passthroughToken)) {

            presenterTypes.remove(PresenterType.ACSP);

            final var error = Err.invalidQueryParameterBuilderWithLocation(FORM_TYPE)
                .withError("Form can only be filed by a valid ACSP").build();
            errors.addError(error);
        }

        // Call out to ExemptUser service
        // Check for exempt presenter types before looking up the user.
        if (!Collections.disjoint(presenterTypes, EXEMPT_USER_PRESENTER_TYPES) && !isExemptUser(userId)) {

            presenterTypes.removeAll(EXEMPT_USER_PRESENTER_TYPES);

            final var error = Err.invalidQueryParameterBuilderWithLocation(FORM_TYPE)
                .withError("Form can only be filed by an exempt user").build();
            errors.addError(error);
        }

        if (errors.hasErrors() && presenterTypes.isEmpty()) {
            return ServiceResult.unprocessable(errors);
        }

        logger.debugContext(userId, "Resolved possible presenter types for form type and user", logMap);

        return ServiceResult.found(presenterTypes);
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
        final String email;
        try {
            email = accountService.getUserEmailByUserId(userId);
        } catch (final URIValidationException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid exempt user URI", exception);
        }

        try {
            return exemptUserService.isExemptUserByEmail(email);
        } catch (final ResponseStatusException exception) {
            if( exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                logger.debugContext(userId, "No exempt user record found for user", logMap);
                return false;
            }
            throw exception;
        }
    }
}
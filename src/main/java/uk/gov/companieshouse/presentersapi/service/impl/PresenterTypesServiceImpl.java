package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.EnumSet;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@Service
public class PresenterTypesServiceImpl implements PresenterTypesService {

    private final PresenterTypeMatrixService presenterTypeMatrixService;
    private final AcspUserService acspUserService;
    private final Logger logger;

    public PresenterTypesServiceImpl(final PresenterTypeMatrixService presenterTypeMatrixService,
        final AcspUserService acspUserService, final Logger logger) {
        this.presenterTypeMatrixService = presenterTypeMatrixService;
        this.acspUserService = acspUserService;
        this.logger = logger;
    }

    @Override
    public EnumSet<PresenterType> getPresenterTypes(final String userId, final String formType,
        final String passthroughToken) {

        final var formTypeEnum = toFormType(formType);
        final var presenterTypes = EnumSet.copyOf(presenterTypeMatrixService.presenterTypesFor(formTypeEnum));

        final Map<String, Object> logMap = Map.of(
            "user_id", userId, "form_type", formType, "presenter_types", presenterTypes.toString());
        logger.debugContext(userId, "Retrieved possible presenter types for form type", logMap);

        // Only call out to the ACSP service if the presenter types include ACSP for this form type
        if (presenterTypes.contains(PresenterType.ACSP) && !isValidAcsp(userId, passthroughToken)) {
            presenterTypes.remove(PresenterType.ACSP);
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
}

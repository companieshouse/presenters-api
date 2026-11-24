package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.identityverification.model.Uvid;
import uk.gov.companieshouse.api.presenters.model.Acsp;
import uk.gov.companieshouse.api.presenters.model.DateOfBirth;
import uk.gov.companieshouse.api.presenters.model.Exempt;
import uk.gov.companieshouse.api.presenters.model.Individual;
import uk.gov.companieshouse.api.presenters.model.PersonName;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotExemptException;
import uk.gov.companieshouse.presentersapi.exception.PresenterNotValidAcspException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotValidException;
import uk.gov.companieshouse.presentersapi.guard.FormTypeGuard;
import uk.gov.companieshouse.presentersapi.guard.Guarded;
import uk.gov.companieshouse.presentersapi.service.AccountService;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.StatementTypeMatrixService;
import uk.gov.companieshouse.presentersapi.utils.RequestHelper;
import uk.gov.companieshouse.service.ServiceResult;

@Service
public class PresenterDetailsServiceImpl implements PresenterDetailsService {

    private static final String INCOMPLETE_INDIVIDUAL_DETAILS_MESSAGE =
        "Identity-verification returned incomplete individual details";
    private static final String INCOMPLETE_ACSP_DETAILS_MESSAGE =
        "ACSP manage users API returned incomplete membership details";

    private final StatementTypeMatrixService statementTypeMatrixService;
    private final IndividualUserService individualUserService;
    private final AcspUserService acspUserService;
    private final AccountService accountService;
    private final ExemptUserService exemptUserService;
    private final PresenterTypeMatrixService presenterTypeMatrixService;
    private final FormTypeGuard formTypeGuard;

    public PresenterDetailsServiceImpl(final StatementTypeMatrixService statementTypeMatrixService,
                                       final IndividualUserService individualUserService,
                                       final AcspUserService acspUserService,
                                       final AccountService accountService,
                                       final ExemptUserService exemptUserService,
                                       final PresenterTypeMatrixService presenterTypeMatrixService,
                                       final FormTypeGuard formTypeGuard) {
        this.statementTypeMatrixService = statementTypeMatrixService;
        this.individualUserService = individualUserService;
        this.acspUserService = acspUserService;
        this.accountService = accountService;
        this.exemptUserService = exemptUserService;
        this.presenterTypeMatrixService = presenterTypeMatrixService;
        this.formTypeGuard = formTypeGuard;
    }

    @Override
    public ServiceResult<PresenterDetails> getPresenterDetails(final String userId, final String formType,
                                                               final String presenterType) {

        final var formTypeEnum = formTypeGuard.fromQuery(formType);
        if (!formTypeEnum.isValid()) {
            return ServiceResult.invalid(Guarded.combinedErrors(formTypeEnum));
        }
        final var presenterTypesForForm = presenterTypeMatrixService.presenterTypesAllowedFor(formTypeEnum.value());

        final var presenterTypeEnum = PresenterType.find(presenterType)
            .orElseThrow(() -> new PresenterTypeNotFoundException("Presenter Type: " + presenterType));
        if (!presenterTypesForForm.contains(presenterTypeEnum)) {
            throw new PresenterTypeNotValidException("Presenter Type is not valid for Form Type");
        }

        final var presenterDetails = new PresenterDetails();
        presenterDetails.setStatements(statementTypeMatrixService
            .statementTypesAllowedFor(presenterTypeEnum, formTypeEnum.value())
            .statementCodes());

        switch (presenterTypeEnum) {
            case ACSP -> {
                final var passthroughToken = RequestHelper.requirePassthroughToken();
                final var acsp = getAcspDetails(userId, passthroughToken);
                presenterDetails.setUserId(userId);
                presenterDetails.setAcsp(acsp);
            }
            case OFFICIAL_RECEIVER -> presenterDetails.setExempt(getExemptDetails(userId));
            default -> {
                presenterDetails.setUserId(userId);
                presenterDetails.setIndividual(getIndividual(userId));
            }
        }
        return ServiceResult.found(presenterDetails);
    }

    private Exempt getExemptDetails(final String userId) {
        final String email;
        try {
            email = accountService.getUserEmailByUserId(userId);
        } catch (final URIValidationException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid exempt user URI", exception);
        }

        final var exemptUser = exemptUserService.getExemptUserByEmail(email)
            .orElseThrow(() -> new PresenterNotExemptException("User is not exempt"));
        final var exempt = new Exempt();
        exempt.setOrganisationName(exemptUser.getOrganisation());
        exempt.setName(exemptUser.getFullName());
        exempt.setEmail(exemptUser.getEmail());
        return exempt;
    }

    private Acsp getAcspDetails(final String userId, final String passthroughToken) {
        final var membership = acspUserService.getAcspMembershipForUser(userId, passthroughToken)
            .filter(PresenterDetailsServiceImpl::isActiveMembershipOfActiveAcsp)
            .orElseThrow(() -> new PresenterNotValidAcspException("User is not associated with an ACSP"));
        validateAcspDetails(membership);

        return mapAcsp(membership);
    }

    private static boolean isActiveMembershipOfActiveAcsp(final AcspMembership membership) {
        return membership.getMembershipStatus() == MembershipStatusEnum.ACTIVE
            && membership.getAcspStatus() == AcspStatusEnum.ACTIVE;
    }

    private static void validateAcspDetails(final AcspMembership membership) {
        if (StringUtils.isAnyBlank(membership.getAcspNumber(), membership.getAcspName(),
            membership.getUserEmail(), membership.getUserDisplayName())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, INCOMPLETE_ACSP_DETAILS_MESSAGE);
        }
    }

    private Individual getIndividual(final String userId) {
        try {
            final var identity = individualUserService.getIdentityByUserId(userId);
            validateIdentityDetails(identity);
            final var uvid = individualUserService.getActiveUvidByIdentityId(identity.getId());
            validateUvid(uvid);

            return mapIndividual(identity, uvid);
        } catch (final URIValidationException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid identity-verification URI", exception);
        }
    }

    private static Individual mapIndividual(final Identity identity, final Uvid uvid) {
        final var currentName = identity.getCurrentName();
        final var dateOfBirth = identity.getDateOfBirth();
        final var individual = new Individual();
        individual.setPersonalCode(uvid.getUvid());
        individual.setVerificationId(identity.getId());
        individual.setName(new PersonName(
            List.copyOf(Objects.requireNonNull(currentName.getForenames())), currentName.getSurname()));
        individual.setDateOfBirth(new DateOfBirth(
            dateOfBirth.getDayOfMonth(), dateOfBirth.getMonthValue(), dateOfBirth.getYear()));
        return individual;
    }

    private static Acsp mapAcsp(final AcspMembership membership) {
        final var acsp = new Acsp();
        acsp.setAcspCode(membership.getAcspNumber());
        acsp.setAcspName(membership.getAcspName());
        acsp.setAcspEmployeeEmail(membership.getUserEmail());
        // Temporarily preserve the display name in surname; future story PRES-543 will address structured names.
        acsp.setPersonName(new PersonName(List.of(), membership.getUserDisplayName()));
        return acsp;
    }

    @SuppressWarnings("java:S2589") // SDK @NotNull annotations do not validate API responses.
    private static void validateIdentityDetails(final Identity identity) {
        if (identity == null || StringUtils.isBlank(identity.getId())
            || identity.getDateOfBirth() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, INCOMPLETE_INDIVIDUAL_DETAILS_MESSAGE);
        }

        final var currentName = identity.getCurrentName();
        if (currentName == null || currentName.getForenames() == null || currentName.getForenames().isEmpty()
            || currentName.getSurname() == null || currentName.getSurname().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, INCOMPLETE_INDIVIDUAL_DETAILS_MESSAGE);
        }
    }

    private static void validateUvid(final Uvid uvid) {
        if (uvid == null || uvid.getUvid() == null || uvid.getUvid().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, INCOMPLETE_INDIVIDUAL_DETAILS_MESSAGE);
        }
    }
}

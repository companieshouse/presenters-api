package uk.gov.companieshouse.presentersapi.service.impl;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.CurrentName;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.identityverification.model.Uvid;
import uk.gov.companieshouse.api.presenters.model.Acsp;
import uk.gov.companieshouse.api.presenters.model.DateOfBirth;
import uk.gov.companieshouse.api.presenters.model.Exempt;
import uk.gov.companieshouse.api.presenters.model.Individual;
import uk.gov.companieshouse.api.presenters.model.PersonName;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotValidException;
import uk.gov.companieshouse.presentersapi.guard.FormTypeGuard;
import uk.gov.companieshouse.presentersapi.guard.Guarded;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.StatementTypeMatrixService;
import uk.gov.companieshouse.service.ServiceResult;

@Service
public class PresenterDetailsServiceImpl implements PresenterDetailsService {

    private final StatementTypeMatrixService statementTypeMatrixService;
    private final IndividualUserService individualUserService;
    private final PresenterTypeMatrixService presenterTypeMatrixService;
    private final FormTypeGuard formTypeGuard;

    public PresenterDetailsServiceImpl(final StatementTypeMatrixService statementTypeMatrixService,
                                       final IndividualUserService individualUserService,
                                       final PresenterTypeMatrixService presenterTypeMatrixService, FormTypeGuard formTypeGuard) {
        this.statementTypeMatrixService = statementTypeMatrixService;
        this.individualUserService = individualUserService;
        this.presenterTypeMatrixService = presenterTypeMatrixService;
        this.formTypeGuard = formTypeGuard;
    }

    @Override
    public ServiceResult<PresenterDetails> getPresenterDetails(final String userId, final String formType, final String presenterType) {

        final var formTypeEnum = formTypeGuard.fromQuery(formType);
        if (!formTypeEnum.isValid()) {
            return ServiceResult.invalid(Guarded.combinedErrors(formTypeEnum));
        }
        final var presenterTypesForForm = EnumSet.copyOf(presenterTypeMatrixService.presenterTypesFor(formTypeEnum.value()));

        final var presenterTypeEnum = PresenterType.find(presenterType)
            .orElseThrow(() -> new PresenterTypeNotFoundException("Presenter Type: " + presenterType));
        if (!presenterTypesForForm.contains(presenterTypeEnum)) {
            throw new PresenterTypeNotValidException("Presenter Type is not valid for Form Type");
        }

        final var presenterDetails = new PresenterDetails();
        presenterDetails.setStatements(statementTypeMatrixService
            .statementTypesFor(presenterTypeEnum, formTypeEnum.value())
            .statementCodes());

        switch (presenterTypeEnum) {
            case ACSP -> {
                final var acsp = new Acsp();
                acsp.setAcspCode("123456-123456");
                acsp.setAcspName("Mr ACSP Ltd");
                acsp.setAcspEmployeeEmail("me@myacsp.co.uk");
                acsp.setPersonName(new PersonName(List.of("John"), "SmithAcsp"));
                presenterDetails.setUserId(userId);
                presenterDetails.setAcsp(acsp);
            }
            case OFFICIAL_RECEIVER -> {
                final var exempt = new Exempt();
                exempt.setOrganisationName("Exemption LTD");
                exempt.setName("Mr Test Exemption");
                exempt.setEmail("me@iamexempt.co.uk");
                presenterDetails.setExempt(exempt);
            }
            default -> {
                presenterDetails.setUserId(userId);
                presenterDetails.setIndividual(getIndividual(userId));
            }
        }
        return ServiceResult.found(presenterDetails);
    }

    private Individual getIndividual(final String userId) {
        try {
            final Identity identity = individualUserService.getIdentityByUserId(userId);
            validateIdentityDetails(identity);
            final Uvid uvid = individualUserService.getActiveUvidByIdentityId(identity.getId());
            validateUvid(uvid);

            final CurrentName currentName = identity.getCurrentName();
            final LocalDate dateOfBirth = identity.getDateOfBirth();
            final var individual = new Individual();
            individual.setPersonalCode(uvid.getUvid());
            individual.setVerificationId(identity.getId());
            individual.setName(new PersonName(List.copyOf(currentName.getForenames()), currentName.getSurname()));
            individual.setDateOfBirth(new DateOfBirth(
                dateOfBirth.getDayOfMonth(), dateOfBirth.getMonthValue(), dateOfBirth.getYear()));
            return individual;
        } catch (final URIValidationException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid identity-verification URI", exception);
        }
    }

    private static void validateIdentityDetails(final Identity identity) {
        if (identity == null || identity.getId() == null || identity.getId().isBlank()
            || identity.getDateOfBirth() == null || identity.getCurrentName() == null
            || identity.getCurrentName().getForenames() == null || identity.getCurrentName().getForenames().isEmpty()
            || identity.getCurrentName().getSurname() == null || identity.getCurrentName().getSurname().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Identity-verification returned incomplete individual details");
        }
    }

    private static void validateUvid(final Uvid uvid) {
        if (uvid == null || uvid.getUvid() == null || uvid.getUvid().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Identity-verification returned incomplete individual details");
        }
    }
}

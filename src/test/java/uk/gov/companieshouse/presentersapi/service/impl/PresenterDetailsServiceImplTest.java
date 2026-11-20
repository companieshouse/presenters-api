package uk.gov.companieshouse.presentersapi.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.CurrentName;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.api.identityverification.model.Uvid;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.exception.PresenterTypeNotValidException;
import uk.gov.companieshouse.presentersapi.guard.FormTypeGuard;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.StatementTypeMatrixService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterDetailsServiceImpl tests")
class PresenterDetailsServiceImplTest {

    private static final String USER_ID = "user123";
    private static final String FORM_TYPE = "AD01";
    private static final String IDENTITY_ID = "identity-123";
    private static final String PERSONAL_CODE = "123456-123456";

    @Mock
    private StatementTypeMatrixService statementTypeMatrixService;
    @Mock
    private IndividualUserService individualUserService;
    @Mock
    private PresenterTypeMatrixService presenterTypeMatrixService;
    @Mock
    private FormGroupFormTypeBiMap formGroupFormTypeBiMap;

    private PresenterDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        final var formTypeGuard = new FormTypeGuard(formGroupFormTypeBiMap);
        service = new PresenterDetailsServiceImpl(
            statementTypeMatrixService, individualUserService, presenterTypeMatrixService, formTypeGuard);
    }

    @Test
    void shouldMapIdentityDetailsToIndividualForSelfFiling() throws URIValidationException {
        givenPresenterTypes(PresenterType.INDIVIDUAL_FILING_FOR_SELF);
        givenIdentityDetails();
        givenStatements(PresenterType.INDIVIDUAL_FILING_FOR_SELF,
            new PresenterStatements(
                EnumSet.noneOf(AssociationStatementType.class),
                EnumSet.noneOf(DeliveryStatementType.class),
                EnumSet.of(VerificationStatementType.VERIFIED_STATEMENT)));

        final PresenterDetails result = service.getPresenterDetails(
            USER_ID, FORM_TYPE, PresenterType.INDIVIDUAL_FILING_FOR_SELF.code()).getData();

        assertThat(result.getUserId(), is(USER_ID));
        assertThat(result.getStatements(), is(List.of("verified-statement")));
        assert result.getIndividual() != null;
        assertThat(result.getIndividual().getPersonalCode(), is(PERSONAL_CODE));
        assertThat(result.getIndividual().getVerificationId(), is(IDENTITY_ID));
        assertThat(result.getIndividual().getName().getForenames(), is(List.of("Ada", "Grace")));
        assertThat(result.getIndividual().getName().getSurname(), is("Lovelace"));
        assertThat(result.getIndividual().getDateOfBirth().getDay(), is(10));
        assertThat(result.getIndividual().getDateOfBirth().getMonth(), is(12));
        assertThat(result.getIndividual().getDateOfBirth().getYear(), is(1815));
        assertNull(result.getAcsp());
        assertNull(result.getExempt());
        verify(individualUserService).getIdentityByUserId(USER_ID);
        verify(individualUserService).getActiveUvidByIdentityId(IDENTITY_ID);
    }

    @Test
    void shouldReturnAcspDetailsWithoutLookingUpIdentity() {
        givenPresenterTypes(PresenterType.ACSP);
        givenStatements(PresenterType.ACSP, emptyStatements());

        final PresenterDetails result = service.getPresenterDetails(
            USER_ID, FORM_TYPE, PresenterType.ACSP.code()).getData();

        assertThat(result.getUserId(), is(USER_ID));
        assertThat(result.getStatements(), is(List.of()));
        assertThat(result.getAcsp().getAcspCode(), is("123456-123456"));
        assertThat(result.getAcsp().getAcspName(), is("Mr ACSP Ltd"));
        verifyNoInteractions(individualUserService);
    }

    @Test
    void shouldReturnOfficialReceiverDetailsWithoutLookingUpIdentity() {
        givenPresenterTypes(PresenterType.OFFICIAL_RECEIVER);
        givenStatements(PresenterType.OFFICIAL_RECEIVER, emptyStatements());

        final PresenterDetails result = service.getPresenterDetails(
            USER_ID, FORM_TYPE, PresenterType.OFFICIAL_RECEIVER.code()).getData();

        assertThat(result.getStatements(), is(List.of()));
        assertThat(result.getExempt().getOrganisationName(), is("Exemption LTD"));
        verifyNoInteractions(individualUserService);
    }

    @ParameterizedTest
    @EnumSource(value = PresenterType.class, names = {"OFFICIAL_RECEIVER", "ACSP"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldLookUpIdentityForEveryIndividualPresenterType(final PresenterType presenterType)
        throws URIValidationException {
        givenPresenterTypes(presenterType);
        givenIdentityDetails();
        givenStatements(presenterType, emptyStatements());

        final PresenterDetails result = service.getPresenterDetails(
            USER_ID, FORM_TYPE, presenterType.code()).getData();

        assertThat(result.getIndividual().getPersonalCode(), is(PERSONAL_CODE));
        assertThat(result.getIndividual().getVerificationId(), is(IDENTITY_ID));
        verify(individualUserService).getIdentityByUserId(USER_ID);
        verify(individualUserService).getActiveUvidByIdentityId(IDENTITY_ID);
    }

    @Test
    void shouldRejectPresenterTypeNotAvailableForForm() {
        givenPresenterTypes(PresenterType.ACSP);
        final var presenterType = PresenterType.OFFICER_EMPLOYEE.code();

        assertThrows(PresenterTypeNotValidException.class,
            () -> service.getPresenterDetails(USER_ID, FORM_TYPE, presenterType));

        verifyNoInteractions(individualUserService, statementTypeMatrixService);
    }

    @Test
    void shouldThrowExceptionWhenPresenterTypeIsUnsupported() {
        givenPresenterTypes(PresenterType.ACSP);

        final var exception = assertThrows(PresenterTypeNotFoundException.class,
            () -> service.getPresenterDetails(USER_ID, FORM_TYPE, "unsupported-type"));

        assertThat(exception.getMessage(), is("Presenter Type: unsupported-type"));
        verifyNoInteractions(individualUserService, statementTypeMatrixService);
    }

    @Test
    void shouldReturnInternalServerErrorWhenIdentityLookupUriIsInvalid() throws URIValidationException {
        givenPresenterTypes(PresenterType.INDIVIDUAL_FILING_FOR_SELF);
        givenStatements(PresenterType.INDIVIDUAL_FILING_FOR_SELF, emptyStatements());
        final var cause = new URIValidationException("Invalid URI");
        when(individualUserService.getIdentityByUserId(USER_ID)).thenThrow(cause);
        final var presenterType = PresenterType.INDIVIDUAL_FILING_FOR_SELF.code();

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterDetails(USER_ID, FORM_TYPE, presenterType));

        assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(exception.getCause(), is(cause));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("incompleteIdentities")
    void shouldReturnBadGatewayWhenIdentityDetailsAreIncomplete(final String scenario, final Identity identity)
        throws URIValidationException {
        givenPresenterTypes(PresenterType.INDIVIDUAL_FILING_FOR_SELF);
        givenStatements(PresenterType.INDIVIDUAL_FILING_FOR_SELF, emptyStatements());
        when(individualUserService.getIdentityByUserId(USER_ID)).thenReturn(identity);
        final var presenterType = PresenterType.INDIVIDUAL_FILING_FOR_SELF.code();

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterDetails(USER_ID, FORM_TYPE, presenterType));

        assertThat(scenario, exception.getStatusCode(), is(HttpStatus.BAD_GATEWAY));
        assertThat(exception.getReason(), is("Identity-verification returned incomplete individual details"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("incompleteUvids")
    void shouldReturnBadGatewayWhenUvidIsIncomplete(final String scenario, final Uvid uvid)
        throws URIValidationException {
        givenPresenterTypes(PresenterType.INDIVIDUAL_FILING_FOR_SELF);
        givenStatements(PresenterType.INDIVIDUAL_FILING_FOR_SELF, emptyStatements());
        when(individualUserService.getIdentityByUserId(USER_ID)).thenReturn(validIdentity());
        when(individualUserService.getActiveUvidByIdentityId(IDENTITY_ID)).thenReturn(uvid);
        final var presenterType = PresenterType.INDIVIDUAL_FILING_FOR_SELF.code();

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterDetails(USER_ID, FORM_TYPE, presenterType));

        assertThat(scenario, exception.getStatusCode(), is(HttpStatus.BAD_GATEWAY));
        assertThat(exception.getReason(), is("Identity-verification returned incomplete individual details"));
    }

    private static Stream<Arguments> incompleteIdentities() {
        return Stream.of(
            Arguments.of("Missing identity", null),
            Arguments.of("Empty identity", new Identity()),
            Arguments.of("Missing identity ID", validIdentity().id(null)),
            Arguments.of("Empty identity ID", validIdentity().id("")),
            Arguments.of("Blank identity ID", validIdentity().id(" ")),
            Arguments.of("Missing date of birth", validIdentity().dateOfBirth(null)),
            Arguments.of("Missing current name", validIdentity().currentName(null)),
            Arguments.of("Missing forenames",
                validIdentity().currentName(new CurrentName().forenames(null).surname("Lovelace"))),
            Arguments.of("Empty forenames",
                validIdentity().currentName(new CurrentName().forenames(List.of()).surname("Lovelace"))),
            Arguments.of("Missing surname",
                validIdentity().currentName(new CurrentName().forenames(List.of("Ada", "Grace")).surname(null))),
            Arguments.of("Empty surname",
                validIdentity().currentName(new CurrentName().forenames(List.of("Ada", "Grace")).surname(""))),
            Arguments.of("Blank surname",
                validIdentity().currentName(new CurrentName().forenames(List.of("Ada", "Grace")).surname(" "))));
    }

    private static Stream<Arguments> incompleteUvids() {
        return Stream.of(
            Arguments.of("Missing UVID", null),
            Arguments.of("Missing personal code", new Uvid()),
            Arguments.of("Empty personal code", new Uvid().uvid("")),
            Arguments.of("Blank personal code", new Uvid().uvid(" ")));
    }

    private static Identity validIdentity() {
        return new Identity()
            .id(IDENTITY_ID)
            .currentName(new CurrentName().forenames(List.of("Ada", "Grace")).surname("Lovelace"))
            .dateOfBirth(LocalDate.of(1815, 12, 10));
    }

    private void givenPresenterTypes(final PresenterType... presenterTypes) {
        when(presenterTypeMatrixService.presenterTypesAllowedFor(FormType.AD01))
            .thenReturn(EnumSet.of(presenterTypes[0], presenterTypes));
    }

    private void givenIdentityDetails() throws URIValidationException {
        when(individualUserService.getIdentityByUserId(USER_ID)).thenReturn(validIdentity());
        when(individualUserService.getActiveUvidByIdentityId(IDENTITY_ID))
            .thenReturn(new Uvid().uvid(PERSONAL_CODE));
    }

    private void givenStatements(final PresenterType presenterType, final PresenterStatements statements) {
        when(statementTypeMatrixService.statementTypesAllowedFor(presenterType, FormType.AD01)).thenReturn(statements);
    }

    private static PresenterStatements emptyStatements() {
        return new PresenterStatements(
            EnumSet.noneOf(AssociationStatementType.class),
            EnumSet.noneOf(DeliveryStatementType.class),
            EnumSet.noneOf(VerificationStatementType.class));
    }
}

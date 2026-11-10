package uk.gov.companieshouse.presentersapi.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.identityverification.model.Identity;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.AccountService;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;
import uk.gov.companieshouse.presentersapi.service.IndividualUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.utils.RequestHelper;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypesServiceImpl tests")
class PresenterTypesServiceImplTest {

    private static final String USER_ID = "user123";
    private static final String USER_EMAIL = "user@example.com";
    private static final String IDENTITY_ID = "identity123";
    private static final String PASSTHROUGH_TOKEN = "passthrough-token";
    private static final String FORM_TYPE_CODE = "AA01";

    @Mock
    private PresenterTypeMatrixService presenterTypeMatrixService;
    @Mock
    private AcspUserService acspUserService;
    @Mock
    private ExemptUserService exemptUserService;
    @Mock
    private AccountService accountService;
    @Mock
    private IndividualUserService individualUserService;
    @Mock
    private Logger logger;

    private MockedStatic<RequestHelper> requestHelperMock;

    private PresenterTypesServiceImpl service;

    @BeforeEach
    void setUp() {
        requestHelperMock = mockStatic(RequestHelper.class);
        requestHelperMock.when(RequestHelper::getChsUserId).thenReturn(USER_ID);
        service = new PresenterTypesServiceImpl(presenterTypeMatrixService, acspUserService, exemptUserService,
                accountService, individualUserService, logger);
    }

    @AfterEach
    void tearDown() {
        requestHelperMock.close();
    }

    @Test
    @DisplayName("should include ACSP when user is a valid ACSP and form type can be filed by an ACSP")
    void shouldIncludeAcspWhenUserIsValidAcspAndFormTypeAllowsAcsp() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.ACTIVE, AcspStatusEnum.ACTIVE));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP)));
    }

    @Test
    @DisplayName("should exclude ACSP when user is not an ACSP member")
    void shouldExcludeAcspWhenUserIsNotAcspMember() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.empty());

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE)));
    }

    @Test
    @DisplayName("should exclude ACSP when user's ACSP membership has been removed")
    void shouldExcludeAcspWhenMembershipRemoved() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.REMOVED, AcspStatusEnum.ACTIVE));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE)));
    }

    @ParameterizedTest(name = "should exclude ACSP when ACSP status is {0}")
    @EnumSource(value = AcspStatusEnum.class, names = {"CEASED", "SUSPENDED"})
    void shouldExcludeAcspWhenAcspIsCeasedOrSuspended(final AcspStatusEnum acspStatus) throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.ACTIVE, acspStatus));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE)));
    }

    @Test
    @DisplayName("should exclude ACSP when the membership has no statuses")
    void shouldExcludeAcspWhenMembershipStatusesMissing() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.ACSP));
        givenMembership(new AcspMembership());

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.noneOf(PresenterType.class)));
    }

    @Test
    @DisplayName("should not include ACSP nor check membership when form type cannot be filed by an ACSP")
    void shouldNotIncludeAcspWhenFormTypeCannotBeFiledByAcsp() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF)));
        verifyNoInteractions(acspUserService);
    }

    @Test
    @DisplayName("should return an empty set when the form type has no presenter types")
    void shouldReturnEmptySetWhenFormTypeHasNoPresenterTypes() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.noneOf(PresenterType.class)));
        verifyNoInteractions(acspUserService);
    }

    @Test
    @DisplayName("should not modify the set returned by the statement matrix")
    void shouldNotModifyMatrixPresenterTypes() throws URIValidationException {
        final var matrixPresenterTypes = EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP);
        givenMatrixPresenterTypes(matrixPresenterTypes);
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.empty());

        service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat("Matrix data should be left intact",
            matrixPresenterTypes, is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP)));
    }

    @Test
    @DisplayName("should return a not found result when form type is unknown")
    void shouldReturnNotFoundWhenFormTypeUnknown() {
        final var result = service.getPresenterTypes(USER_ID, "UNKNOWN-FORM", PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.NOT_FOUND));
        assertThat(result.getData(), is(nullValue()));
        assertThat(result.getErrors().size(), is(1));
        assertThat(result.getErrors().containsError(
            Err.serviceErrBuilder().withError("Form type not found").build()), is(true));
        verifyNoInteractions(presenterTypeMatrixService, acspUserService, individualUserService,
            accountService, exemptUserService, logger);
        requestHelperMock.verifyNoInteractions();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "different-user")
    void shouldResolvePresenterTypesUsingAuthenticatedUserWhenSuppliedUserIdDiffers(final String userId)
        throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.ACTIVE, AcspStatusEnum.ACTIVE));
        when(accountService.getUserEmailByUserId(USER_ID)).thenReturn(USER_EMAIL);
        when(exemptUserService.isExemptUserByEmail(USER_EMAIL)).thenReturn(true);

        final var result = service.getPresenterTypes(userId, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF,
            PresenterType.ACSP, PresenterType.OFFICIAL_RECEIVER)));
        verify(individualUserService).getActiveUvidByIdentityId(IDENTITY_ID);
    }

    @ParameterizedTest
    @EnumSource(value = PresenterType.class, names = {"OFFICIAL_RECEIVER", "ACSP"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldLookUpIdentityWhenOtherPresenterTypeIsPresent(final PresenterType presenterType)
        throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(presenterType));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(presenterType)));
        verify(individualUserService).getActiveUvidByIdentityId(IDENTITY_ID);
    }

    @ParameterizedTest
    @EnumSource(value = PresenterType.class, names = {"OFFICIAL_RECEIVER", "ACSP"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldLookUpIdentityWhenOtherPresenterTypeIsMixedWithExcludedTypes(final PresenterType presenterType)
        throws URIValidationException {
        final var presenterTypes = EnumSet.of(presenterType, PresenterType.OFFICIAL_RECEIVER, PresenterType.ACSP);
        givenMatrixPresenterTypes(presenterTypes);
        givenMembership(membership(MembershipStatusEnum.ACTIVE, AcspStatusEnum.ACTIVE));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(presenterTypes));
        verify(individualUserService).getActiveUvidByIdentityId(IDENTITY_ID);
    }

    @ParameterizedTest
    @MethodSource("presenterTypesWithoutOtherTypes")
    void shouldSkipIdentityLookupWhenNoOtherPresenterTypeIsPresent(final EnumSet<PresenterType> presenterTypes)
        throws URIValidationException {
        givenMatrixPresenterTypes(presenterTypes);
        if (presenterTypes.contains(PresenterType.ACSP)) {
            givenMembership(membership(MembershipStatusEnum.ACTIVE, AcspStatusEnum.ACTIVE));
        }

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(presenterTypes));
        verifyNoInteractions(individualUserService);
    }

    @Test
    void shouldReturnInternalServerErrorWhenIdentityLookupUriIsInvalid() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));
        final var cause = new URIValidationException("Invalid URI");
        when(individualUserService.getIdentityByUserId(USER_ID)).thenThrow(cause);

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN));

        assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(exception.getCause(), is(cause));
    }

    @Test
    void shouldPropagateIdentityLookupFailure() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));
        final var cause = new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity-verification API is unavailable");
        when(individualUserService.getIdentityByUserId(USER_ID)).thenThrow(cause);

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN));

        assertThat(exception, is(cause));
    }

    @Test
    void shouldReturnInternalServerErrorWhenActiveUvidLookupUriIsInvalid() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));
        final var cause = new URIValidationException("Invalid URI");
        when(individualUserService.getActiveUvidByIdentityId(IDENTITY_ID)).thenThrow(cause);

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN));

        assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(exception.getCause(), is(cause));
    }

    @Test
    void shouldRemoveOtherPresenterTypesWhenNoActiveUvidIsFound() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));
        final var cause = new ResponseStatusException(HttpStatus.NOT_FOUND, "No active UVID was found for the identity");
        when(individualUserService.getActiveUvidByIdentityId(IDENTITY_ID)).thenThrow(cause);

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.noneOf(PresenterType.class)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPreserveAcspAndOfficialReceiverWhenIndividualIsNotVerified(final boolean identityMissing)
        throws URIValidationException {
        final var matrixPresenterTypes = EnumSet.allOf(PresenterType.class);
        givenMatrixPresenterTypes(matrixPresenterTypes);
        givenMembership(membership(MembershipStatusEnum.ACTIVE, AcspStatusEnum.ACTIVE));
        final var cause = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        if (identityMissing) {
            when(individualUserService.getIdentityByUserId(USER_ID)).thenThrow(cause);
        } else {
            when(individualUserService.getActiveUvidByIdentityId(IDENTITY_ID)).thenThrow(cause);
        }

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.ACSP, PresenterType.OFFICIAL_RECEIVER)));
        assertThat("Matrix data should be left intact", matrixPresenterTypes, is(EnumSet.allOf(PresenterType.class)));
    }

    @Test
    void shouldNotRestoreAcspWhenNeitherAcspNorIndividualIsVerified() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.allOf(PresenterType.class));
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.empty());
        when(individualUserService.getIdentityByUserId(USER_ID))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Identity not found"));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.OFFICIAL_RECEIVER)));
    }

    @Test
    void shouldPropagateActiveUvidLookupFailure() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));
        final var cause = new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity-verification API is unavailable");
        when(individualUserService.getActiveUvidByIdentityId(IDENTITY_ID)).thenThrow(cause);

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN));

        assertThat(exception, is(cause));
    }

    private static Stream<EnumSet<PresenterType>> presenterTypesWithoutOtherTypes() {
        return Stream.of(
            EnumSet.noneOf(PresenterType.class),
            EnumSet.of(PresenterType.OFFICIAL_RECEIVER),
            EnumSet.of(PresenterType.ACSP),
            EnumSet.of(PresenterType.OFFICIAL_RECEIVER, PresenterType.ACSP));
    }

    @Test
    @DisplayName("should return OFFICIAL_RECEIVER when user is exempt")
    void shouldReturnOfficialReceiverWhenUserIsExempt() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        when(accountService.getUserEmailByUserId(USER_ID)).thenReturn(USER_EMAIL);
        when(exemptUserService.isExemptUserByEmail(USER_EMAIL)).thenReturn(true);

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.of(PresenterType.OFFICIAL_RECEIVER)));
    }
    @Test
    @DisplayName("should not return OFFICIAL_RECEIVER when user is not exempt")
    void shouldNotReturnOfficialReceiverWhenUserIsNotExempt() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        when(accountService.getUserEmailByUserId(USER_ID)).thenReturn(USER_EMAIL);
        when(exemptUserService.isExemptUserByEmail(USER_EMAIL)).thenReturn(false);

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.noneOf(PresenterType.class)));
    }

    @Test
    @DisplayName("should not return OFFICIAL_RECEIVER when exempt lookup fails")
    void shouldNotReturnOfficialReceiverWhenExemptLookupFails() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        when(accountService.getUserEmailByUserId(USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Downstream unavailable"));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(EnumSet.noneOf(PresenterType.class)));
    }

    private void givenMatrixPresenterTypes(final EnumSet<PresenterType> presenterTypes) throws URIValidationException {
        when(presenterTypeMatrixService.presenterTypesFor(FormType.from(FORM_TYPE_CODE)))
            .thenReturn(presenterTypes);
        if (presenterTypes.stream().anyMatch(presenterType ->
            presenterType != PresenterType.OFFICIAL_RECEIVER && presenterType != PresenterType.ACSP)) {
            final var identity = new Identity();
            identity.setId(IDENTITY_ID);
            when(individualUserService.getIdentityByUserId(USER_ID)).thenReturn(identity);
        }
    }

    private void givenMembership(final AcspMembership membership) {
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.of(membership));
    }

    private static AcspMembership membership(final MembershipStatusEnum membershipStatus,
        final AcspStatusEnum acspStatus) {
        return new AcspMembership().membershipStatus(membershipStatus).acspStatus(acspStatus);
    }
}

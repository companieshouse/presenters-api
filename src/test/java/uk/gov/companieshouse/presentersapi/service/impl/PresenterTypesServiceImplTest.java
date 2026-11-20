package uk.gov.companieshouse.presentersapi.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.FormGroup;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.error.RequestParamErrors;
import uk.gov.companieshouse.presentersapi.guard.FormTypeGuard;
import uk.gov.companieshouse.presentersapi.service.AccountService;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.utils.RequestHelper;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypesServiceImpl tests")
class PresenterTypesServiceImplTest {

    private static final String USER_ID = "user123";
    private static final String USER_EMAIL = "user@example.com";
    private static final String PASSTHROUGH_TOKEN = "passthrough-token";
    private static final String FORM_TYPE_CODE = "AA01";

    @Mock
    private PresenterTypeMatrixService presenterTypeMatrixService;
    @Mock
    private FormGroupFormTypeBiMap formGroupFormTypeBiMap;
    @Mock
    private AcspUserService acspUserService;
    @Mock
    private ExemptUserService exemptUserService;
    @Mock
    private AccountService accountService;
    @Mock
    private Logger logger;

    private MockedStatic<RequestHelper> requestHelperMock;

    private PresenterTypesServiceImpl service;

    @BeforeEach
    void setUp() {
        requestHelperMock = mockStatic(RequestHelper.class);
        requestHelperMock.when(RequestHelper::getChsUserId).thenReturn(USER_ID);
        service = new PresenterTypesServiceImpl(presenterTypeMatrixService, acspUserService, exemptUserService,
                accountService, logger, new FormTypeGuard(formGroupFormTypeBiMap));
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

        assertFoundResult(result, EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
    }

    @Test
    @DisplayName("should exclude ACSP when user is not an ACSP member")
    void shouldExcludeAcspWhenUserIsNotAcspMember() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.empty());

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.of(PresenterType.OFFICER_EMPLOYEE));
    }

    @Test
    @DisplayName("should exclude ACSP when user's ACSP membership has been removed")
    void shouldExcludeAcspWhenMembershipRemoved() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.REMOVED, AcspStatusEnum.ACTIVE));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.of(PresenterType.OFFICER_EMPLOYEE));
    }

    @ParameterizedTest(name = "should exclude ACSP when ACSP status is {0}")
    @EnumSource(value = AcspStatusEnum.class, names = {"CEASED", "SUSPENDED"})
    void shouldExcludeAcspWhenAcspIsCeasedOrSuspended(final AcspStatusEnum acspStatus) throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.ACTIVE, acspStatus));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.of(PresenterType.OFFICER_EMPLOYEE));
    }

    @Test
    @DisplayName("should exclude ACSP when the membership has no statuses")
    void shouldExcludeAcspWhenMembershipStatusesMissing() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.ACSP, PresenterType.OFFICER_EMPLOYEE));
        givenMembership(new AcspMembership());

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.of(PresenterType.OFFICER_EMPLOYEE));
    }

    @Test
    @DisplayName("should not include ACSP nor check membership when form type cannot be filed by an ACSP")
    void shouldNotIncludeAcspWhenFormTypeCannotBeFiledByAcsp() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));
        verifyNoInteractions(acspUserService);
    }

    @Test
    @DisplayName("should return an empty set when the form type has no presenter types")
    void shouldReturnEmptySetWhenFormTypeHasNoPresenterTypes() throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.noneOf(PresenterType.class));
        verifyNoInteractions(acspUserService);
    }

    @Test
    void shouldReturnEmptySetWhenKnownFormTypeHasNoGroupMapping() {
        when(formGroupFormTypeBiMap.formGroupFor(FormType.WU15)).thenReturn(null);
        when(presenterTypeMatrixService.presenterTypesAllowedFor(FormType.WU15))
            .thenReturn(EnumSet.noneOf(PresenterType.class));

        final var result = service.getPresenterTypes(USER_ID, "WU15", PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.noneOf(PresenterType.class));
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
    @DisplayName("should reject an unknown form type")
    void shouldRejectUnknownFormType() {
        final var result = service.getPresenterTypes(USER_ID, "UNKNOWN-FORM", PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.VALIDATION_ERROR));
        assertThat(result.getErrors().size(), is(1));
        assertThat(result.getErrors().containsError(RequestParamErrors.formTypeInvalid("UNKNOWN-FORM")), is(true));
        verifyNoInteractions(formGroupFormTypeBiMap, presenterTypeMatrixService, acspUserService,
            accountService, exemptUserService);
    }

    @ParameterizedTest
    @EnumSource(value = FormGroup.class, names = {"UNMARKED", "EXEMPT_FORM"})
    void shouldRejectFormTypeWhenFormGroupIsUnsupported(final FormGroup group) {
        when(formGroupFormTypeBiMap.formGroupFor(FormType.AA01)).thenReturn(group);

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result.getStatus(), is(ServiceResultStatus.VALIDATION_ERROR));
        assertThat(result.getErrors().size(), is(1));
        assertThat(result.getErrors().containsError(
            RequestParamErrors.formTypeNotSupported(FORM_TYPE_CODE, group.code())), is(true));
        verifyNoInteractions(presenterTypeMatrixService, acspUserService, accountService, exemptUserService);
    }

    @ParameterizedTest
    @EnumSource(value = PresenterType.class, names = {"OFFICIAL_RECEIVER", "ACSP"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnPresenterTypeAllowedByMatrix(final PresenterType presenterType)
        throws URIValidationException {
        givenMatrixPresenterTypes(EnumSet.of(presenterType));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.of(presenterType));
    }

    @Test
    @DisplayName("should return OFFICIAL_RECEIVER when user is exempt")
    void shouldReturnOfficialReceiverWhenUserIsExempt() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        when(accountService.getUserEmailByUserId(USER_ID)).thenReturn(USER_EMAIL);
        when(exemptUserService.isExemptUserByEmail(USER_EMAIL)).thenReturn(true);

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.of(PresenterType.OFFICIAL_RECEIVER));
    }

    @Test
    @DisplayName("should not return OFFICIAL_RECEIVER when user is not exempt")
    void shouldNotReturnOfficialReceiverWhenUserIsNotExempt() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        when(accountService.getUserEmailByUserId(USER_ID)).thenReturn(USER_EMAIL);
        when(exemptUserService.isExemptUserByEmail(USER_EMAIL)).thenReturn(false);

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.noneOf(PresenterType.class));
    }

    @Test
    @DisplayName("should not return OFFICIAL_RECEIVER when exempt lookup fails")
    void shouldNotReturnOfficialReceiverWhenExemptLookupFails() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        when(accountService.getUserEmailByUserId(USER_ID)).thenReturn(USER_EMAIL);

        when(exemptUserService.isExemptUserByEmail(USER_EMAIL))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No exempt user record found"));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertFoundResult(result, EnumSet.noneOf(PresenterType.class));
    }

    @Test
    @DisplayName("should propagate NOT_FOUND when account lookup fails")
    void shouldPropagateNotFoundWhenAccountLookupFails() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        final var cause = new ResponseStatusException(HttpStatus.NOT_FOUND, "No account record found");
        when(accountService.getUserEmailByUserId(USER_ID)).thenThrow(cause);

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN));

        assertThat(exception, is(cause));
    }

    @Test
    @DisplayName("should return INTERNAL_SERVER_ERROR when account URI is invalid for exempt lookup")
    void shouldReturnInternalServerErrorWhenAccountUriIsInvalidForExemptLookup() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        final var cause = new URIValidationException("Invalid URI");
        when(accountService.getUserEmailByUserId(USER_ID)).thenThrow(cause);

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN));

        assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(exception.getReason(), is("Invalid exempt user URI"));
        assertThat(exception.getCause(), is(cause));
    }

    @Test
    @DisplayName("should throw an INTERNAL_SERVER_ERROR when exempt lookup fails with an unexpected error")
    void shouldThrowAnInternalServerErrorWhenExemptLookupFails() throws Exception {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));
        when(accountService.getUserEmailByUserId(USER_ID)).thenReturn(USER_EMAIL);
        when(exemptUserService.isExemptUserByEmail(USER_EMAIL)).thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"));

        final var exception = assertThrows(ResponseStatusException.class,
            () -> service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN));

        assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(exception.getReason(), is("Unexpected error"));
    }

    private void givenMatrixPresenterTypes(final EnumSet<PresenterType> presenterTypes) {
        when(formGroupFormTypeBiMap.formGroupFor(FormType.AA01)).thenReturn(FormGroup.FIRM_DELIVERY);
        when(presenterTypeMatrixService.presenterTypesAllowedFor(FormType.find(FORM_TYPE_CODE).orElseThrow()))
            .thenReturn(presenterTypes);
    }

    private void givenMembership(final AcspMembership membership) {
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.of(membership));
    }

    private static AcspMembership membership(final MembershipStatusEnum membershipStatus,
        final AcspStatusEnum acspStatus) {
        return new AcspMembership().membershipStatus(membershipStatus).acspStatus(acspStatus);
    }

    private static void assertFoundResult(final ServiceResult<EnumSet<PresenterType>> result,
        final EnumSet<PresenterType> expectedPresenterTypes) {
        assertThat(result.getStatus(), is(ServiceResultStatus.FOUND));
        assertThat(result.getData(), is(expectedPresenterTypes));
    }

}

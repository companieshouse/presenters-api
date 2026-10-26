package uk.gov.companieshouse.presentersapi.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException;
import uk.gov.companieshouse.presentersapi.service.AcspUserService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypesServiceImpl tests")
class PresenterTypesServiceImplTest {

    private static final String USER_ID = "user123";
    private static final String PASSTHROUGH_TOKEN = "passthrough-token";
    private static final String FORM_TYPE_CODE = "AA01";

    @Mock
    private PresenterTypeMatrixService presenterTypeMatrixService;
    @Mock
    private AcspUserService acspUserService;
    @Mock
    private Logger logger;

    private PresenterTypesServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PresenterTypesServiceImpl(presenterTypeMatrixService, acspUserService, logger);
    }

    @Test
    @DisplayName("should include ACSP when user is a valid ACSP and form type can be filed by an ACSP")
    void shouldIncludeAcspWhenUserIsValidAcspAndFormTypeAllowsAcsp() {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.ACTIVE, AcspStatusEnum.ACTIVE));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result, is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP)));
    }

    @Test
    @DisplayName("should exclude ACSP when user is not an ACSP member")
    void shouldExcludeAcspWhenUserIsNotAcspMember() {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.empty());

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result, is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE)));
    }

    @Test
    @DisplayName("should exclude ACSP when user's ACSP membership has been removed")
    void shouldExcludeAcspWhenMembershipRemoved() {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.REMOVED, AcspStatusEnum.ACTIVE));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result, is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE)));
    }

    @ParameterizedTest(name = "should exclude ACSP when ACSP status is {0}")
    @EnumSource(value = AcspStatusEnum.class, names = {"CEASED", "SUSPENDED"})
    void shouldExcludeAcspWhenAcspIsCeasedOrSuspended(final AcspStatusEnum acspStatus) {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP));
        givenMembership(membership(MembershipStatusEnum.ACTIVE, acspStatus));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result, is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE)));
    }

    @Test
    @DisplayName("should exclude ACSP when the membership has no statuses")
    void shouldExcludeAcspWhenMembershipStatusesMissing() {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.ACSP));
        givenMembership(new AcspMembership());

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result, is(EnumSet.noneOf(PresenterType.class)));
    }

    @Test
    @DisplayName("should not include ACSP nor check membership when form type cannot be filed by an ACSP")
    void shouldNotIncludeAcspWhenFormTypeCannotBeFiledByAcsp() {
        givenMatrixPresenterTypes(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result, is(EnumSet.of(PresenterType.INDIVIDUAL_FILING_FOR_SELF)));
        verifyNoInteractions(acspUserService);
    }

    @Test
    @DisplayName("should return an empty set when the form type has no presenter types")
    void shouldReturnEmptySetWhenFormTypeHasNoPresenterTypes() {
        givenMatrixPresenterTypes(EnumSet.noneOf(PresenterType.class));

        final var result = service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat(result, is(EnumSet.noneOf(PresenterType.class)));
        verifyNoInteractions(acspUserService);
    }

    @Test
    @DisplayName("should not modify the set returned by the statement matrix")
    void shouldNotModifyMatrixPresenterTypes() {
        final var matrixPresenterTypes = EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP);
        givenMatrixPresenterTypes(matrixPresenterTypes);
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.empty());

        service.getPresenterTypes(USER_ID, FORM_TYPE_CODE, PASSTHROUGH_TOKEN);

        assertThat("Matrix data should be left intact",
            matrixPresenterTypes, is(EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP)));
    }

    @Test
    @DisplayName("should throw FormTypeNotFoundException when form type is unknown")
    void shouldThrowFormTypeNotFoundExceptionWhenFormTypeUnknown() {
        final var exception = assertThrows(FormTypeNotFoundException.class,
            () -> service.getPresenterTypes(USER_ID, "UNKNOWN-FORM", PASSTHROUGH_TOKEN));

        assertThat(exception.getMessage(), is("Form Type: UNKNOWN-FORM"));
        verifyNoInteractions(presenterTypeMatrixService, acspUserService);
    }


    private void givenMatrixPresenterTypes(final EnumSet<PresenterType> presenterTypes) {
        when(presenterTypeMatrixService.presenterTypesFor(eq(FormType.from(FORM_TYPE_CODE))))
            .thenReturn(presenterTypes);
    }

    private void givenMembership(final AcspMembership membership) {
        when(acspUserService.getAcspMembershipForUser(USER_ID, PASSTHROUGH_TOKEN)).thenReturn(Optional.of(membership));
    }

    private static AcspMembership membership(final MembershipStatusEnum membershipStatus,
        final AcspStatusEnum acspStatus) {
        return new AcspMembership().membershipStatus(membershipStatus).acspStatus(acspStatus);
    }
}

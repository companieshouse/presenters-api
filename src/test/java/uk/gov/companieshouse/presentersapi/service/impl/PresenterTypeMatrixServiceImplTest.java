package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupPresenterTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.FormGroup;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenterTypeMatrixServiceImpl tests")
class PresenterTypeMatrixServiceImplTest {

    @Mock
    private FormGroupFormTypeBiMap formGroupFormTypeBiMap;
    @Mock
    private FormGroupPresenterTypeBiMap formGroupPresenterTypeBiMap;

    private PresenterTypeMatrixServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PresenterTypeMatrixServiceImpl(formGroupFormTypeBiMap, formGroupPresenterTypeBiMap);
    }

    @Test
    @DisplayName("presenterTypesFor(FormType) resolves the FormGroup, then its PresenterTypes set")
    void presenterTypesForFormTypeResolvesViaFormGroup() {
        final var expected = EnumSet.of(PresenterType.OFFICER_EMPLOYEE, PresenterType.ACSP_SOLE_TRADER);
        when(formGroupFormTypeBiMap.formGroupFor(FormType.AD01)).thenReturn(FormGroup.FIRM_DELIVERY);
        when(formGroupPresenterTypeBiMap.presenterTypesFor(FormGroup.FIRM_DELIVERY)).thenReturn(expected);

        assertThat(service.presenterTypesFor(FormType.AD01)).isEqualTo(expected);
        verify(formGroupFormTypeBiMap).formGroupFor(FormType.AD01);
        verify(formGroupPresenterTypeBiMap).presenterTypesFor(FormGroup.FIRM_DELIVERY);
    }

    @Test
    @DisplayName("presenterTypesFor(FormType) returns an empty set when the FormType has no FormGroup")
    void presenterTypesForFormTypeWithoutFormGroupReturnsEmpty() {
        when(formGroupFormTypeBiMap.formGroupFor(FormType.WU15)).thenReturn(null);

        assertThat(service.presenterTypesFor(FormType.WU15)).isEmpty();
        verifyNoInteractions(formGroupPresenterTypeBiMap);
    }

    @Test
    @DisplayName("presenterTypesFor(FormType) returns an empty set when the FormType is null")
    void shouldReturnEmptySetWhenFormTypeIsNull() {
        assertThat(service.presenterTypesFor(null)).isEmpty();
        verifyNoInteractions(formGroupPresenterTypeBiMap);
    }
}

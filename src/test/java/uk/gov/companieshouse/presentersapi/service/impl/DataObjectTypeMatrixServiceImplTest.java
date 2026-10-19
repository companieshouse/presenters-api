package uk.gov.companieshouse.presentersapi.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeDataObjectTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;

@ExtendWith(MockitoExtension.class)
class DataObjectTypeMatrixServiceImplTest {

    @Mock
    private PresenterTypeDataObjectTypeBiMap presenterTypeDataObjectTypeBiMap;

    private DataObjectTypeMatrixServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DataObjectTypeMatrixServiceImpl(presenterTypeDataObjectTypeBiMap);
    }

    @Test
    void shouldReturnDataObjectTypesForOfficialReceiver() {
        final var expected = EnumSet.of(DataObjectType.EMAIL);
        when(presenterTypeDataObjectTypeBiMap.dataObjectTypesFor(PresenterType.OFFICIAL_RECEIVER))
            .thenReturn(expected);

        assertThat(service.dataObjectTypesFor(PresenterType.OFFICIAL_RECEIVER), is(expected));
        verify(presenterTypeDataObjectTypeBiMap).dataObjectTypesFor(PresenterType.OFFICIAL_RECEIVER);
    }

    @Test
    void shouldRejectNullPresenterType() {
        when(presenterTypeDataObjectTypeBiMap.dataObjectTypesFor(null))
            .thenThrow(new NullPointerException("key"));

        assertThrows(NullPointerException.class, () -> service.dataObjectTypesFor(null));
        verify(presenterTypeDataObjectTypeBiMap).dataObjectTypesFor(null);
    }
}

package uk.gov.companieshouse.presentersapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.impl.DataObjectTypeMatrixServiceImpl;

/**
 * Verifies DataObjectTypeMatrixServiceImpl is wired with the BiMap populated from reference data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DataObjectTypeMatrixServiceImplIT {

    @Autowired
    private DataObjectTypeMatrixServiceImpl service;

    @Test
    void shouldResolveDataObjectTypesForOfficialReceiver() {
        assertThat(service.dataObjectTypesFor(PresenterType.OFFICIAL_RECEIVER),
            is(EnumSet.of(DataObjectType.ORGANISATION_NAME, DataObjectType.NAME, DataObjectType.EMAIL)));
    }

    @Test
    void shouldRejectNullPresenterType() {
        assertThrows(NullPointerException.class, () -> service.dataObjectTypesFor(null));
    }
}

package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;

@DisplayName("PresenterTypesServiceImpl tests")
class PresenterTypesServiceImplTest {

    private final PresenterTypesServiceImpl service = new PresenterTypesServiceImpl();

    @Test
    @DisplayName("should return all presenter types")
    void testGetPresenterTypesReturnsAllTypes() {
        var result = service.getPresenterTypes("user123", "form-abc");

        assertEquals(EnumSet.allOf(PresenterType.class), result);
    }
}

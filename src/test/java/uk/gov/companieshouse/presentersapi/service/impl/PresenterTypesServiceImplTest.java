package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import uk.gov.companieshouse.api.presenters.model.PresenterTypes;

@DisplayName("PresenterTypesServiceImpl tests")
class PresenterTypesServiceImplTest {

    private final PresenterTypesServiceImpl service = new PresenterTypesServiceImpl();

    @Test
    @DisplayName("should return presenter types list")
    void testGetPresenterTypesReturnsList() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertNotNull(result);
        assertNotNull(result.getTypes());
        assertFalse(result.getTypes().isEmpty());
    }

    @Test
    @DisplayName("should return 7 presenter types")
    void testGetPresenterTypesReturnsEightTypes() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertEquals(7, result.getTypes().size());
    }

    @Test
    @DisplayName("should contain all expected presenter types")
    void testContainsExpectedTypes() {

        List<String> expectedTypes = List.of(
            "officer-employee",
            "corporate-officer-employee",
            "exempt-presenter",
            "acsp-sole-trader",
            "acsp-employee",
            "individual-filing-for-self",
            "individual-filing-for-someone-else"
        );

        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertTrue(result.getTypes().containsAll(expectedTypes),
            "Expected presenter types not found");
    }

    @Test
    @DisplayName("should return immutable list for thread-safety")
    void testReturnedListIsImmutable() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");
        List<String> types = result.getTypes();

        assertThrows(UnsupportedOperationException.class, () -> types.add("new-type"));
    }

    @Test
    @DisplayName("should not return null types list")
    void testTypesNotNull() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertNotNull(result.getTypes());
    }

    @Test
    @DisplayName("should return non-empty types")
    void testTypesNotEmpty() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertFalse(result.getTypes().isEmpty());
    }

    @Test
    @DisplayName("should have no duplicate types")
    void testNoDuplicateTypes() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        List<String> types = result.getTypes();
        long uniqueCount = types.stream().distinct().count();

        assertEquals(types.size(), uniqueCount);
    }

    @Test
    @DisplayName("should not contain empty strings in types")
    void testNoEmptyTypeStrings() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        result.getTypes().forEach(type -> {
            assertNotNull(type);
            assertFalse(type.isEmpty());
            assertFalse(type.isBlank());
        });
    }
}

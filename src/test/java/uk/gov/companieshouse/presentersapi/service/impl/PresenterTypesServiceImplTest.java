package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;

import uk.gov.companieshouse.api.presenters.model.PresenterTypes;

@DisplayName("PresenterTypesServiceImpl tests")
class PresenterTypesServiceImplTest {

    private final PresenterTypesServiceImpl service = new PresenterTypesServiceImpl();

    private static final String[] EXPECTED_TYPES = {
        "officer-employee",
        "corporate-officer-employee",
        "exempt-presenter",
        "exempt-form",
        "acsp-sole-trader",
        "acsp-employee",
        "individual-filing-for-self",
        "individual-filing-for-someone-else"
    };

    @Test
    @DisplayName("should return presenter types list")
    void testGetPresenterTypesReturnsList() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertNotNull(result);
        assertNotNull(result.getTypes());
        assertFalse(result.getTypes().isEmpty());
    }

    @Test
    @DisplayName("should return 8 presenter types")
    void testGetPresenterTypesReturnsEightTypes() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertEquals(8, result.getTypes().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "officer-employee",
        "corporate-officer-employee",
        "exempt-presenter",
        "exempt-form",
        "acsp-sole-trader",
        "acsp-employee",
        "individual-filing-for-self",
        "individual-filing-for-someone-else"
    })
    @DisplayName("should contain all expected presenter types")
    void testContainsExpectedTypes(String presenterType) {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertTrue(result.getTypes().contains(presenterType),
            "Expected presenter type '" + presenterType + "' not found");
    }

    @ParameterizedTest
    @CsvSource({
        "user123, form-abc",
        "user456, form-xyz",
        "user789, form-123",
        "different-user, different-form"
    })
    @DisplayName("should return same presenter types for different userId/formType")
    void testParameterIndependence(String userId, String formType) {
        PresenterTypes result = service.getPresenterTypes(userId, formType);

        assertNotNull(result);
        assertEquals(8, result.getTypes().size());
        assertTrue(result.getTypes().contains("officer-employee"));
    }

    @ParameterizedTest
    @MethodSource("provideNullParameters")
    @DisplayName("should handle null parameters")
    void testWithNullParameters(String userId, String formType) {
        PresenterTypes result = service.getPresenterTypes(userId, formType);

        assertNotNull(result);
        assertEquals(8, result.getTypes().size());
    }

    static Stream<Object[]> provideNullParameters() {
        return Stream.of(
            new Object[]{null, "form-abc"},
            new Object[]{"user123", null},
            new Object[]{null, null}
        );
    }

    @Test
    @DisplayName("should return types in correct order")
    void testTypesOrderIsCorrect() {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        List<String> types = result.getTypes();
        assertArrayEquals(EXPECTED_TYPES, types.toArray());
    }

    @ParameterizedTest
    @MethodSource("provideTypeIndexes")
    @DisplayName("should have types at expected indexes")
    void testTypeAtIndex(int index, String expectedType) {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertEquals(expectedType, result.getTypes().get(index));
    }

    static Stream<Object[]> provideTypeIndexes() {
        return Stream.of(
            new Object[]{0, "officer-employee"},
            new Object[]{1, "corporate-officer-employee"},
            new Object[]{2, "exempt-presenter"},
            new Object[]{3, "exempt-form"},
            new Object[]{4, "acsp-sole-trader"},
            new Object[]{5, "acsp-employee"},
            new Object[]{6, "individual-filing-for-self"},
            new Object[]{7, "individual-filing-for-someone-else"}
        );
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
        assertTrue(result.getTypes().size() > 0);
    }

    @ParameterizedTest
    @MethodSource("provideMultipleCalls")
    @DisplayName("should return consistent results across multiple calls")
    void testConsistentResults(int callNumber) {
        PresenterTypes result = service.getPresenterTypes("user123", "form-abc");

        assertNotNull(result);
        assertEquals(8, result.getTypes().size());
        assertEquals("officer-employee", result.getTypes().get(0));
    }

    static Stream<Integer> provideMultipleCalls() {
        return Stream.of(1, 2, 3, 4, 5);
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

    @Test
    @DisplayName("should implement PresenterTypesService interface")
    void testImplementsInterface() {
        PresenterTypesServiceImpl impl = new PresenterTypesServiceImpl();

        assertTrue(impl instanceof uk.gov.companieshouse.presentersapi.service.PresenterTypesService);
    }
}

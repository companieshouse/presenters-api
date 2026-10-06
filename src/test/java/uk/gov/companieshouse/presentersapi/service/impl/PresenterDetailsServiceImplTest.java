package uk.gov.companieshouse.presentersapi.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import uk.gov.companieshouse.api.presenters.model.PresenterDetails;

@DisplayName("PresenterDetailsServiceImpl tests")
class PresenterDetailsServiceImplTest {

    private final PresenterDetailsServiceImpl service = new PresenterDetailsServiceImpl();

    @Test
    @DisplayName("should return presenter details")
    void testGetPresenterDetailsReturnsPresenterDetails() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertNotNull(result);
        assertNotNull(result.getStatements());
        assertNotNull(result.getExempt());
    }

    @Test
    @DisplayName("should return exactly two statements")
    void testGetPresenterDetailsReturnsTwoStatements() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertEquals(2, result.getStatements().size());
    }

    @Test
    @DisplayName("should contain all expected statements")
    void testContainsExpectedStatements() {
        List<String> expectedStatements = List.of(
            "exempt-statement",
            "delivery-on-behalf-of-firm"
        );

        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertTrue(result.getStatements().containsAll(expectedStatements),
            "Expected statements not found");
    }

    @Test
    @DisplayName("should return immutable statements list for thread-safety")
    void testReturnedStatementsListIsImmutable() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");
        List<String> statements = result.getStatements();

        assertThrows(UnsupportedOperationException.class, () -> statements.add("new-statement"));
    }

    @Test
    @DisplayName("should not return null statements list")
    void testStatementsNotNull() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertNotNull(result.getStatements());
    }

    @Test
    @DisplayName("should return non-empty statements")
    void testStatementsNotEmpty() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        assertFalse(result.getStatements().isEmpty());
    }

    @Test
    @DisplayName("should have no duplicate statements")
    void testNoDuplicateStatements() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        List<String> statements = result.getStatements();
        long uniqueCount = statements.stream().distinct().count();

        assertEquals(statements.size(), uniqueCount);
    }

    @Test
    @DisplayName("should set correct email address")
    void testExemptEmail() {
        PresenterDetails result = service.getPresenterDetails("user123", "form-abc", "officer-employee");

        assert result.getExempt() != null;
        assertEquals("me@iamexempt.co.uk", result.getExempt().getEmail());
    }
}
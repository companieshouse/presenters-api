Type: java-api

# Copilot Instructions

## Java Coding Style Preferences

- Use explicit types for fields, method parameters, and return types (`var` is not legal in these positions).
- Prefer `var` for local variable types when the initialiser can be inferred by the compiler and the type is obvious from the right-hand side of the assignment.
- Otherwise use an explicit local variable type instead of `var` only when the compiler cannot infer it.
- Include `final` in local variable and method parameter definitions to ensure immutability. You may combine `final` with `var` for local variables (e.g., `final var x = ...;`).
- Avoid wildcard imports; use explicit imports across Java code.

## Unit Test Coding Style Preferences (Priority)

- JUnit 5 is the preferred testing framework for unit tests. JUnit 4 is acceptable for legacy code, but new tests should be written in JUnit 5.
- Prefer Mockito for mocking, stubbing, and verification in unit tests.
- Prefer Hamcrest `assertThat(...)` style assertions in unit tests.
- Prefer explicit static imports for matchers, e.g. `import static org.hamcrest.CoreMatchers.is;`.
- Prefer more natural language matchers (e.g., `is`, `isA`, `notNullValue`) over basic equality assertions (`equalTo`),
  unless the latter provides clearer intent in a specific case.
- Prefer assertions wrapped in `is`, even if redundant, for better natural readability (e.g.,
  `assertThat(result, is(notNullValue()))` rather than `assertThat(result, notNullValue())`).
- Avoid the test antipattern of using `Mockito.verify` to check interactions with mocks that were stubbed to return
  specific values in the same test, as this can lead to brittle tests that are tightly coupled to implementation
  details. Instead, focus on asserting the expected outcomes based on the stubbed behaviour.
- When it does not expose too much knowledge of the internal implementation, use `Mockito.verifyNoInteractions` or
  `Mockito.verifyNoMoreInteractions` to ensure that no unexpected interactions with mocks occur in a test, rather than
  verifying specific interactions that are already defined by stubs.
- Prefer immutable test data (using `final`) to prevent accidental modifications during test execution.
- Use `notNullValue()` when asserting that a value is not null: `assertThat(result, is(notNullValue()))`.
- Use `isA(SomeClass.class)` when you want to assert both non-null status and type in a single statement.
- Use explicit null checks sparingly; prefer positive assertions (e.g., assert the object has expected properties rather
  than just asserting it exists).

### Example Imports

```java
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
```

### Test Isolation

- Ensure each test is independent and can run in any order without affecting other tests: JUnit does not guarantee test execution order, so tests should not rely on side effects from other tests. Each test should set up all the data and mocks it needs.
- Avoid sharing mutable state between tests; use `@Before` or `@BeforeEach` methods to reset state if necessary.
- Clean up resources (e.g., close streams, reset mocks) in `@After` or `@AfterEach` methods when necessary.

### Test Naming Conventions

- Use descriptive test method names that clearly express the expected behavior and conditions being tested.
- Follow the pattern: `should<ExpectedOutcome>When<Condition>` (e.g., `shouldReturnEmptyListWhenFileNotFound`).
- Alternatively, use: `test<MethodUnderTest><Scenario>` for simpler cases (e.g., `testProcessDocumentSuccess`).
- Avoid generic names like `test1`, `testMethod`, or `testValid`. The test name should serve as documentation.

### Assertion Messages

- When it adds clarity, include concise, descriptive messages in assertions to aid debugging when tests fail. Assertion messages should describe *what* should have happened at the domain level, not *how* the assertion works —- similar to how comments should enhance understanding rather than restate code.
- Use the optional message parameter in Hamcrest assertions: `assertThat("User should be inactive", user.isActive(), is(false))`.
- Craft messages that explain what was expected and why, not just what the values are.

### Exception Testing

- Use `assertThrows` (JUnit 5) or `@Test(expected = ...)` (JUnit 4) to verify that methods throw the correct exceptions.
- When testing exceptions, validate both the exception type and its message or cause using lambda expressions:
  ```java
  final var exception = assertThrows(IllegalArgumentException.class, () -> service.process(null));
  assertThat(exception.getMessage(), containsString("Input cannot be null"));
```

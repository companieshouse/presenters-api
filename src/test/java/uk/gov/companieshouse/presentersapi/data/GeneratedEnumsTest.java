package uk.gov.companieshouse.presentersapi.data;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.FormGroup;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;

/**
 * Exercises the generated enums' code lookup: every constant round-trips through its code,
 * and an unknown code is rejected. Relies on the enums produced from enum-generator-config.yaml.
 */
class GeneratedEnumsTest {

    static class GeneratedEnums implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ParameterDeclarations parameters, final ExtensionContext context) {
            return Stream.of(
                enumArguments("FormGroup", FormGroup.values(), FormGroup::from, FormGroup::code),
                enumArguments("FormType", FormType.values(), FormType::from, FormType::code),
                enumArguments("PresenterType", PresenterType.values(), PresenterType::from, PresenterType::code),
                enumArguments("DeliveryStatementType", DeliveryStatementType.values(),
                    DeliveryStatementType::from, DeliveryStatementType::code),
                enumArguments("VerificationStatementType", VerificationStatementType.values(),
                    VerificationStatementType::from, VerificationStatementType::code),
                enumArguments("AssociationStatementType", AssociationStatementType.values(),
                    AssociationStatementType::from, AssociationStatementType::code),
                enumArguments("DataObjectType", DataObjectType.values(), DataObjectType::from, DataObjectType::code));
        }

        private static <E extends Enum<E>> Arguments enumArguments(
                final String name, final E[] constants, final Function<String, E> from, final Function<E, String> code) {
            return Arguments.of(name, constants, from, code);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GeneratedEnums.class)
    <E extends Enum<E>> void shouldReturnSameConstantWhenLookingUpItsCode(
            final String name, final E[] constants, final Function<String, E> from, final Function<E, String> code) {
        assertThat(constants.length > 0, is(true));
        for (final var constant : constants) {
            assertThat(from.apply(code.apply(constant)), is(constant));
        }
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GeneratedEnums.class)
    <E extends Enum<E>> void shouldRejectCodeWhenCodeIsUnknown(
            final String name, final E[] constants, final Function<String, E> from, final Function<E, String> code) {
        final var exception = assertThrows(IllegalArgumentException.class, () -> from.apply("no-such-code"));

        assertThat(exception.getMessage(), containsString("Unknown " + name + ": no-such-code"));
    }
}

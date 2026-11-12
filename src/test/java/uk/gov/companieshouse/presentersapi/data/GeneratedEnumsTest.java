package uk.gov.companieshouse.presentersapi.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
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
 * Exercises the generated enums' non-throwing code lookup: every constant round-trips through its code,
 * and unknown or null codes yield an empty result. Relies on the enums produced from enum-generator-config.yaml.
 */
class GeneratedEnumsTest {

    static class GeneratedEnums implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ParameterDeclarations parameters, final ExtensionContext context) {
            return Stream.of(
                enumArguments("FormGroup", FormGroup.values(), FormGroup::find, FormGroup::code),
                enumArguments("FormType", FormType.values(), FormType::find, FormType::code),
                enumArguments("PresenterType", PresenterType.values(), PresenterType::find, PresenterType::code),
                enumArguments("DeliveryStatementType", DeliveryStatementType.values(),
                    DeliveryStatementType::find, DeliveryStatementType::code),
                enumArguments("VerificationStatementType", VerificationStatementType.values(),
                    VerificationStatementType::find, VerificationStatementType::code),
                enumArguments("AssociationStatementType", AssociationStatementType.values(),
                    AssociationStatementType::find, AssociationStatementType::code),
                enumArguments("DataObjectType", DataObjectType.values(), DataObjectType::find, DataObjectType::code));
        }

        private static <E extends Enum<E>> Arguments enumArguments(
                final String name, final E[] constants, final Function<String, Optional<E>> find,
                final Function<E, String> code) {
            return Arguments.of(name, constants, find, code);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GeneratedEnums.class)
    <E extends Enum<E>> void shouldFindSameConstantWhenLookingUpItsCode(
            final String name, final E[] constants, final Function<String, Optional<E>> find,
            final Function<E, String> code) {
        assertThat(constants.length > 0, is(true));
        for (final var constant : constants) {
            assertThat(find.apply(code.apply(constant)), is(Optional.of(constant)));
        }
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GeneratedEnums.class)
    <E extends Enum<E>> void shouldReturnEmptyWhenCodeIsUnknown(
            final String name, final E[] constants, final Function<String, Optional<E>> find,
            final Function<E, String> code) {
        assertThat(find.apply("no-such-code"), is(Optional.empty()));
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GeneratedEnums.class)
    <E extends Enum<E>> void shouldReturnEmptyWhenCodeIsNull(
            final String name, final E[] constants, final Function<String, Optional<E>> find,
            final Function<E, String> code) {
        assertThat(find.apply(null), is(Optional.empty()));
    }
}

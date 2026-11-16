package uk.gov.companieshouse.presentersapi.guard;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;
import uk.gov.companieshouse.presentersapi.error.RequestBodyErrors;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;

/**
 * Resolves the statement and data object type lists of a validation request into enum constants, without
 * throwing. Each list yields its resolved value or one error identifying distinct rejected values, following the rules in
 * {@link Guards#codeList}.
 */
public final class RequestListGuard {

    private RequestListGuard() {
    }

    /**
     * @param statementIds the statement codes from the request, may be null
     * @return the statements grouped by category when valid, otherwise the statement list error
     */
    public static Guarded<PresenterStatements> guardStatements(final List<String> statementIds) {
        return Guards.<Enum<?>>codeList(statementIds, RequestListGuard::findStatement,
                                             RequestBodyErrors::statementTypesMissing,
                                             RequestBodyErrors::statementTypesNotValid,
                                             RequestBodyErrors::statementTypesDuplicated)
                          .map(RequestListGuard::toPresenterStatements);
    }

    /**
     * @param dataTypes the data object type codes from the request, may be null
     * @return the data object types when valid, otherwise the data object type list error
     */
    public static Guarded<EnumSet<DataObjectType>> guardDataObjectTypes(final List<String> dataTypes) {
        return Guards.<DataObjectType>codeList(dataTypes, DataObjectType::find,
                                                    RequestBodyErrors::dataObjectTypesMissing,
                                                    RequestBodyErrors::dataObjectTypesNotValid,
                                                    RequestBodyErrors::dataObjectTypesDuplicated)
                          .map(EnumSet::copyOf);
    }

    private static Optional<? extends Enum<?>> findStatement(final String code) {
        final Optional<? extends Enum<?>> association = AssociationStatementType.find(code);
        if (association.isPresent()) {
            return association;
        }
        final Optional<? extends Enum<?>> delivery = DeliveryStatementType.find(code);
        if (delivery.isPresent()) {
            return delivery;
        }
        return VerificationStatementType.find(code);
    }

    static PresenterStatements toPresenterStatements(final List<Enum<?>> resolved) {
        final var association = EnumSet.noneOf(AssociationStatementType.class);
        final var delivery = EnumSet.noneOf(DeliveryStatementType.class);
        final var verification = EnumSet.noneOf(VerificationStatementType.class);
        for (final var constant : resolved) {
            switch (constant) {
                case AssociationStatementType type -> association.add(type);
                case DeliveryStatementType type -> delivery.add(type);
                case VerificationStatementType type -> verification.add(type);
                default -> throw new IllegalStateException("Unexpected statement type " + constant);
            }
        }
        return new PresenterStatements(association, delivery, verification);
    }
}

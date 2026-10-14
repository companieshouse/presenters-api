package uk.gov.companieshouse.presentersapi.model.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;

/**
 * Groups the association, delivery and verification statement types that apply to a PresenterType.
 * <p>
 * Instances are immutable: the sets are copied on construction and again on every access.
 *
 * @param associationStatements  the association statement types, never null
 * @param deliveryStatements     the delivery statement types, never null
 * @param verificationStatements the verification statement types, never null
 */
public record PresenterStatements(
    EnumSet<AssociationStatementType> associationStatements,
    EnumSet<DeliveryStatementType> deliveryStatements,
    EnumSet<VerificationStatementType> verificationStatements) {

    /**
     * Creates an instance holding copies of the given sets. Empty sets are allowed.
     *
     * @throws NullPointerException if any set is null
     */
    public PresenterStatements {
        associationStatements = EnumSet.copyOf(Objects.requireNonNull(associationStatements));
        deliveryStatements = EnumSet.copyOf(Objects.requireNonNull(deliveryStatements));
        verificationStatements = EnumSet.copyOf(Objects.requireNonNull(verificationStatements));
    }

    /**
     * @return a new, modifiable copy of the AssociationStatementType constants; changing it does not affect this instance
     */
    @Override
    public EnumSet<AssociationStatementType> associationStatements() {
        return EnumSet.copyOf(associationStatements);
    }

    /**
     * @return a new, modifiable copy of the DeliveryStatementType constants; changing it does not affect this instance
     */
    @Override
    public EnumSet<DeliveryStatementType> deliveryStatements() {
        return EnumSet.copyOf(deliveryStatements);
    }

    /**
     * @return a new, modifiable copy of the VerificationStatementType constants; changing it does not affect this instance
     */
    @Override
    public EnumSet<VerificationStatementType> verificationStatements() {
        return EnumSet.copyOf(verificationStatements);
    }

    /**
     * Combines all statement codes into a flat list suitable for an API response.
     * Codes are ordered by association, delivery, then verification, with enum
     * declaration order preserved within each category and duplicates removed.
     *
     * @return an unmodifiable list of distinct statement codes, empty when no statements are present
     */
    public List<String> statementCodes() {
        return Stream.of(
                         associationStatements.stream().map(AssociationStatementType::code),
                         deliveryStatements.stream().map(DeliveryStatementType::code),
                         verificationStatements.stream().map(VerificationStatementType::code))
                     .flatMap(stream -> stream)
                     .distinct()
                     .toList();
    }
}

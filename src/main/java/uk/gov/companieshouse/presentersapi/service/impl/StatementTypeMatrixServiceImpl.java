package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupDeliveryStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeAssociationStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeDeliveryStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeVerificationStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;
import uk.gov.companieshouse.presentersapi.service.StatementTypeMatrixService;

@Service
public class StatementTypeMatrixServiceImpl implements StatementTypeMatrixService {
    private final PresenterTypeAssociationStatementTypeBiMap presenterTypeAssociationStatementTypeBiMap;
    private final PresenterTypeDeliveryStatementTypeBiMap presenterTypeDeliveryStatementTypeBiMap;
    private final PresenterTypeVerificationStatementTypeBiMap presenterTypeVerificationStatementTypeBiMap;
    private final FormGroupFormTypeBiMap formGroupFormTypeBiMap;
    private final FormGroupDeliveryStatementTypeBiMap formGroupDeliveryStatementTypeBiMap;

    public StatementTypeMatrixServiceImpl(
        final PresenterTypeAssociationStatementTypeBiMap presenterTypeAssociationStatementTypeBiMap,
        final PresenterTypeDeliveryStatementTypeBiMap presenterTypeDeliveryStatementTypeBiMap,
        final PresenterTypeVerificationStatementTypeBiMap presenterTypeVerificationStatementTypeBiMap,
        final FormGroupFormTypeBiMap formGroupFormTypeBiMap,
        final FormGroupDeliveryStatementTypeBiMap formGroupDeliveryStatementTypeBiMap) {
        this.presenterTypeAssociationStatementTypeBiMap = presenterTypeAssociationStatementTypeBiMap;
        this.presenterTypeDeliveryStatementTypeBiMap = presenterTypeDeliveryStatementTypeBiMap;
        this.presenterTypeVerificationStatementTypeBiMap = presenterTypeVerificationStatementTypeBiMap;
        this.formGroupFormTypeBiMap = formGroupFormTypeBiMap;
        this.formGroupDeliveryStatementTypeBiMap = formGroupDeliveryStatementTypeBiMap;
    }

    /**
     * Returns the union of applicable statement types in one value, merging presenter-specific and form-group
     * delivery statements. This is distinct from {@link #validStatementCombinationsFor(PresenterType, FormType)},
     * which returns alternatives separately for exact matching.
     *
     * @param presenterType the non-null PresenterType constant to look up
     * @param formType the non-null FormType constant to look up
     * @return the PresenterStatements for the given presenterType and formType
     * @throws NullPointerException if formType is null and presenterType is not
     *     {@link PresenterType#INDIVIDUAL_FILING_FOR_SELF}
     */
    @Override
    public PresenterStatements statementTypesAllowedFor(final PresenterType presenterType, final FormType formType) {
        final var associationStatements = presenterTypeAssociationStatementTypeBiMap.associationStatementTypesFor(
            presenterType);
        final var verificationStatements = presenterTypeVerificationStatementTypeBiMap.verificationStatementTypesFor(
            presenterType);
        final var deliveryStatements = EnumSet.noneOf(DeliveryStatementType.class);
        if (presenterType != PresenterType.INDIVIDUAL_FILING_FOR_SELF) {
            deliveryStatements.addAll(presenterTypeDeliveryStatementTypeBiMap.deliveryStatementTypesFor(presenterType));
            final var formGroup = formGroupFormTypeBiMap.formGroupFor(formType);

            if (formGroup != null) {
                final var formGroupDeliveryStatements = formGroupDeliveryStatementTypeBiMap.deliveryStatementTypesFor(
                    formGroup);
                deliveryStatements.addAll(formGroupDeliveryStatements);
            }
        }

        return new PresenterStatements(associationStatements, deliveryStatements, verificationStatements);
    }

    /**
     * Returns separate valid alternatives for exact matching; unlike {@link #statementTypesAllowedFor}, delivery
     * statements are not merged across presenter and form-group mappings.
     */
    @Override
    public Set<PresenterStatements> validStatementCombinationsFor(
        final PresenterType presenterType, final FormType formType) {
        final var associationStatements = presenterTypeAssociationStatementTypeBiMap.associationStatementTypesFor(
            presenterType);
        final var verificationStatements = presenterTypeVerificationStatementTypeBiMap.verificationStatementTypesFor(
            presenterType);
        final var deliveryAlternatives = EnumSet.noneOf(DeliveryStatementType.class);

        if (presenterType == PresenterType.ACSP || presenterType == PresenterType.OFFICIAL_RECEIVER) {
            final var formGroup = formGroupFormTypeBiMap.formGroupFor(formType);
            if (formGroup != null) {
                deliveryAlternatives.addAll(formGroupDeliveryStatementTypeBiMap.deliveryStatementTypesFor(formGroup));
            }
        } else if (presenterType != PresenterType.INDIVIDUAL_FILING_FOR_SELF) {
            deliveryAlternatives.addAll(presenterTypeDeliveryStatementTypeBiMap.deliveryStatementTypesFor(presenterType));
        }

        final Set<PresenterStatements> combinations = new HashSet<>();
        if (deliveryAlternatives.isEmpty()) {
            combinations.add(new PresenterStatements(associationStatements,
                EnumSet.noneOf(DeliveryStatementType.class), verificationStatements));
        } else {
            for (final var deliveryStatement : deliveryAlternatives) {
                combinations.add(new PresenterStatements(associationStatements, EnumSet.of(deliveryStatement),
                    verificationStatements));
            }
        }
        return Set.copyOf(combinations);
    }
}

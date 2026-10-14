package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.EnumSet;
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
     * @param presenterType the non-null PresenterType constant to look up
     * @param formType the non-null FormType constant to look up
     * @return the PresenterStatements for the given presenterType and formType
     * @throws NullPointerException if formType is null and presenterType is not
     *     {@link PresenterType#INDIVIDUAL_FILING_FOR_SELF}
     */
    @Override
    public PresenterStatements statementTypesFor(final PresenterType presenterType, final FormType formType) {
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
}

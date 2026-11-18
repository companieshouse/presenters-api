package uk.gov.companieshouse.presentersapi.service;

import java.util.Set;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;

public interface StatementTypeMatrixService {
    /**
     * Returns the union of applicable statement types in one value. Delivery statements from the presenter mapping
     * and form-group mapping are merged; this is not a set of valid combinations and must not be used for exact
     * matching against a request.
     *
     * @param presenterType the non-null PresenterType constant to look up
     * @param formType the FormType constant to look up
     * @return the merged PresenterStatements for the given presenterType and formType
     */
    PresenterStatements statementTypesAllowedFor(PresenterType presenterType, FormType formType);

    /**
     * Returns the valid statement combinations for the given presenter and form. Each element contains all
     * applicable association and verification statements, and exactly one delivery alternative when applicable.
     * When no delivery statement applies, the set contains one combination with an empty delivery set.
     * Because {@link PresenterStatements} is a value record, exact matching can be performed with
     * {@link Set#contains(Object)}.
     * Unlike {@link #statementTypesAllowedFor(PresenterType, FormType)}, this method does not merge delivery
     * alternatives into one value: ACSP and official receiver use form-group delivery alternatives, while other
     * presenters use their presenter-specific delivery mapping.
     *
     * @param presenterType the non-null PresenterType constant to look up
     * @param formType the FormType constant to look up when resolving ACSP or official receiver delivery alternatives
     * @return an immutable, non-empty set of valid PresenterStatements combinations
     */
    Set<PresenterStatements> validStatementCombinationsFor(PresenterType presenterType, FormType formType);
}

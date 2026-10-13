package uk.gov.companieshouse.presentersapi.service;

import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;

public interface StatementTypeMatrixService {
    PresenterStatements statementTypesFor(PresenterType presenterType, FormType formType);
}

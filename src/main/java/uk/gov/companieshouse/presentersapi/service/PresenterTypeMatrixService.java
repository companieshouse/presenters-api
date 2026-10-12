package uk.gov.companieshouse.presentersapi.service;

import java.util.EnumSet;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;

public interface PresenterTypeMatrixService {
    EnumSet<PresenterType> presenterTypesFor(final FormType formType);
}

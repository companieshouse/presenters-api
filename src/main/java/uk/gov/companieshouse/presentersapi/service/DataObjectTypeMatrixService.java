package uk.gov.companieshouse.presentersapi.service;

import java.util.EnumSet;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;

public interface DataObjectTypeMatrixService {
    EnumSet<DataObjectType> dataObjectTypesFor(PresenterType presenterType);
}

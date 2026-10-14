package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.EnumSet;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeDataObjectTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.DataObjectTypeMatrixService;

@Service
public class DataObjectTypeMatrixServiceImpl implements DataObjectTypeMatrixService {
    private final PresenterTypeDataObjectTypeBiMap presenterTypeDataObjectTypeBiMap;

    public DataObjectTypeMatrixServiceImpl(final PresenterTypeDataObjectTypeBiMap presenterTypeDataObjectTypeBiMap) {
        this.presenterTypeDataObjectTypeBiMap = presenterTypeDataObjectTypeBiMap;
    }

    /**
     * @param presenterType the non-null PresenterType constant to look up
     * @return the DataObjectType constants mapped to the presenter type, or an empty set if it has no mapping
     */
    @Override
    public EnumSet<DataObjectType> dataObjectTypesFor(final PresenterType presenterType) {
        return presenterTypeDataObjectTypeBiMap.dataObjectTypesFor(presenterType);
    }
}

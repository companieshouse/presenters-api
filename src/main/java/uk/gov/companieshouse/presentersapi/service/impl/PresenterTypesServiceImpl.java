package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.EnumSet;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@Service
public class PresenterTypesServiceImpl implements PresenterTypesService {

    @Override
    public EnumSet<PresenterType> getPresenterTypes(final String userId, final String formType) {

        return EnumSet.allOf(PresenterType.class);
    }
}

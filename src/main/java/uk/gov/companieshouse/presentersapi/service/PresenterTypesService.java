package uk.gov.companieshouse.presentersapi.service;

import java.util.EnumSet;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;

public interface PresenterTypesService {

    /**
     * Get the available presenter types for a given user and form type.
     *
     * @param userId   the userId
     * @param formType the form type
     * @return the available presenter types
     */
    EnumSet<PresenterType> getPresenterTypes(String userId, String formType);
}

package uk.gov.companieshouse.presentersapi.service;

import java.util.EnumSet;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;

public interface PresenterTypesService {

    /**
     * Get the available presenter types for a given user and form type.
     *
     * <p>The presenter types permitted for the form type by the statement matrix are returned, except that
     * {@link PresenterType#ACSP} is only included when the user is an active member of an active ACSP.</p>
     *
     * @param userId           the userId
     * @param formType         the form type code
     * @param passthroughToken the ERIC passthrough token identifying the user to downstream APIs
     * @return the available presenter types
     * @throws uk.gov.companieshouse.presentersapi.exception.FormTypeNotFoundException if the form type is unknown
     */
    EnumSet<PresenterType> getPresenterTypes(String userId, String formType, String passthroughToken);
}

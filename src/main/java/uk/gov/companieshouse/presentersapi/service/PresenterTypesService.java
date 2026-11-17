package uk.gov.companieshouse.presentersapi.service;

import java.util.EnumSet;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.service.ServiceResult;

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
     * @return the available presenter types, a validation error if the form type is missing, unknown or belongs to
     *     an unsupported form group, or an unprocessable result if no presenter types remain
     */
    ServiceResult<EnumSet<PresenterType>> getPresenterTypes(String userId, String formType, String passthroughToken);
}

package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.EnumSet;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupPresenterTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;

@Service
public class PresenterTypeMatrixServiceImpl implements PresenterTypeMatrixService {
    private final FormGroupFormTypeBiMap formGroupFormTypeBiMap;
    private final FormGroupPresenterTypeBiMap formGroupPresenterTypeBiMap;

    public PresenterTypeMatrixServiceImpl(final FormGroupFormTypeBiMap formGroupFormTypeBiMap,
        final FormGroupPresenterTypeBiMap formGroupPresenterTypeBiMap) {
        this.formGroupFormTypeBiMap = formGroupFormTypeBiMap;
        this.formGroupPresenterTypeBiMap = formGroupPresenterTypeBiMap;
    }

    /**
     * Resolves the FormType's FormGroup, then the PresenterTypes for that FormGroup. Should only return an empty set if
     * the FormType has no FormGroup; since FormType is an enum whose values are generated from reference data YAML, in
     * practice this should never happen for non-null FormType values.
     *
     * @param formType the FormType constant to lookup
     * @return the set of PresenterType constants for the FormType's FormGroup, empty if the FormType has no FormGroup
     *     or is null.
     */
    @Override
    public EnumSet<PresenterType> presenterTypesFor(final FormType formType) {
        final var formGroup = formGroupFormTypeBiMap.formGroupFor(formType);
        return formGroup == null ? EnumSet.noneOf(PresenterType.class)
                   : formGroupPresenterTypeBiMap.presenterTypesFor(formGroup);
    }
}

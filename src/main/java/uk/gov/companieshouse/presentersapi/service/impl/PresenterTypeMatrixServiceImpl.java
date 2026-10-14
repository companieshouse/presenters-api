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
     * Resolves the FormType's FormGroup, then the PresenterTypes for that FormGroup.
     *
     * @param formType the non-null FormType constant to look up
     * @return the PresenterType constants for the FormType's FormGroup, or an empty set if that group has no
     *     associated presenter types
     * @throws NullPointerException if formType is null
     */
    @Override
    public EnumSet<PresenterType> presenterTypesFor(final FormType formType) {
        final var formGroup = formGroupFormTypeBiMap.formGroupFor(formType);
        return formGroup == null ? EnumSet.noneOf(PresenterType.class)
                   : formGroupPresenterTypeBiMap.presenterTypesFor(formGroup);
    }
}

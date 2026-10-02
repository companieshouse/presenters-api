package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.presenters.model.PresenterTypes;
import uk.gov.companieshouse.presentersapi.service.PresenterTypesService;

@Service
public class PresenterTypesServiceImpl implements PresenterTypesService {

    @Override
    public PresenterTypes getPresenterTypes(String userId, String formType) {
        PresenterTypes presenterTypes = new PresenterTypes();
        presenterTypes.setTypes(List.of(
            "officer-employee",
            "corporate-officer-employee",
            "exempt-presenter",
            "exempt-form",
            "acsp-sole-trader",
            "acsp-employee",
            "individual-filing-for-self",
            "individual-filing-for-someone-else"
        ));
        return presenterTypes;
    }
}

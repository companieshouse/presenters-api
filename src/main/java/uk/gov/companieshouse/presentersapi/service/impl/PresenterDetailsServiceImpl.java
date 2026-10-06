package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.presenters.model.Exempt;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;

@Service
public class PresenterDetailsServiceImpl implements PresenterDetailsService {

    @Override
    public PresenterDetails getPresenterDetails(String userId, String formType, String presenterType) {

        List<String> statements = List.of("exempt-statement",
            "delivery-on-behalf-of-firm");

        Exempt exempt = new Exempt();
        exempt.setOrganisationName("Exemption LTD");
        exempt.setName("Mr Test Exemption");
        exempt.setEmail("me@iamexempt.co.uk");

        PresenterDetails presenterDetails = new PresenterDetails();
        presenterDetails.setStatements(statements);
        presenterDetails.setExempt(exempt);

        return presenterDetails;
    }
}

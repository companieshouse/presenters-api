package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.presenters.model.Acsp;
import uk.gov.companieshouse.api.presenters.model.DateOfBirth;
import uk.gov.companieshouse.api.presenters.model.Exempt;
import uk.gov.companieshouse.api.presenters.model.Individual;
import uk.gov.companieshouse.api.presenters.model.PersonName;
import uk.gov.companieshouse.api.presenters.model.PresenterDetails;
import uk.gov.companieshouse.presentersapi.service.PresenterDetailsService;

@Service
public class PresenterDetailsServiceImpl implements PresenterDetailsService {

    @Override
    public PresenterDetails getPresenterDetails(String userId, String formType, String presenterType) {

        PresenterDetails presenterDetails = new PresenterDetails();

        switch (presenterType) {
            case "individual-filing-for-self" -> {

                List<String> statements = List.of("officer-employee-statement",
                    "delivery-on-behalf-of-individual",
                    "verified-statement");

                Individual individual = new Individual();
                individual.setPersonalCode("123456");
                individual.setName(new PersonName(List.of("John"), "Smith"));
                individual.setVerificationId("1234");

                presenterDetails.setUserId(userId);
                presenterDetails.setStatements(statements);
                presenterDetails.setIndividual(individual);
            }
            case "officer-employee" -> {

                List<String> statements = List.of("officer-employee-statement",
                    "delivery-on-behalf-of-firm",
                    "verified-statement");

                Individual individual = new Individual();
                individual.setPersonalCode("123456");
                individual.setName(new PersonName(List.of("John"), "Scott"));
                individual.setDateOfBirth(new DateOfBirth(1, 1, 1980));
                individual.setVerificationId("1234");

                presenterDetails.setUserId(userId);
                presenterDetails.setStatements(statements);
                presenterDetails.setIndividual(individual);
            }
            case "acsp-employee" -> {

                List<String> statements = List.of("ascp-employee-statement",
                    "delivery-on-behalf-of-firm");

                Acsp acsp = new Acsp();
                acsp.setAcspCode("123456-123456");
                acsp.setAcspName("Mr ACSP Ltd");
                acsp.setAcspEmployeeEmail("me@myacsp.co.uk");
                acsp.setPersonName(new PersonName(List.of("John"), "SmithAcsp"));

                presenterDetails.setUserId(userId);
                presenterDetails.setStatements(statements);
                presenterDetails.setAcsp(acsp);
            }
            case "official-receiver" -> {
                List<String> statements = List.of("exempt-statement",
                    "delivery-on-behalf-of-firm");

                Exempt exempt = new Exempt();
                exempt.setOrganisationName("Exemption LTD");
                exempt.setName("Mr Test Exemption");
                exempt.setEmail("me@iamexempt.co.uk");

                presenterDetails.setStatements(statements);
                presenterDetails.setExempt(exempt);
            }
            default -> throw new IllegalStateException("Unexpected value: " + presenterType);
        }

        return presenterDetails;
    }
}

package uk.gov.companieshouse.presentersapi.service.impl;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;

@Service
public class PresenterValidationServiceImpl implements PresenterValidationService {

    @Override
    public ValidationResponse validatePresenterDetails(ValidationRequest validationRequest) {

        return ValidationResponse.VALID;
    }
}

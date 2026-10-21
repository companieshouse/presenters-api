package uk.gov.companieshouse.presentersapi.service.impl;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;

@Service
public class PresenterValidationServiceImpl implements PresenterValidationService {

    @Override
    public ValidationResponse validatePresenterDetails(ValidationRequest validationRequest) {

        ValidationResponse validationResponse = new ValidationResponse();

        if ("AD01".equals(validationRequest.getFormType())) {

            validationResponse.setResult(ValidationResponse.ResultEnum.VALID);
        } else if ("AD02".equals(validationRequest.getFormType())) {

            validationResponse.setResult(ValidationResponse.ResultEnum.INVALID);
            validationResponse.setReason("Statement form type mismatch");
        }

        return validationResponse;
    }
}

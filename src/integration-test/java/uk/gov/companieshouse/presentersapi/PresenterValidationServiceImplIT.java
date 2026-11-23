package uk.gov.companieshouse.presentersapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.ServiceResultStatus;
import uk.gov.companieshouse.service.rest.err.Err;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PresenterValidationServiceImplIT {

    @Autowired
    private PresenterValidationService presenterValidationService;

    @Test
    void shouldValidateAnExactOfficerEmployeeCombination() {
        final var result = validate("AD01",
            List.of("personal-code", "name", "date-of-birth"),
            List.of("officer-employee-statement", "delivery-on-behalf-of-firm", "verified-statement"));

        assertEquals(ServiceResultStatus.FOUND, result.getStatus());
        assertEquals(ValidationResponse.ResultEnum.VALID, result.getData().getResult());
    }

    @Test
    void shouldValidateIndividualFilingWithoutADeliveryStatement() {
        final var result = validate("DS01",
            List.of("personal-code", "name"),
            List.of("verified-statement"));

        assertEquals(ServiceResultStatus.FOUND, result.getStatus());
        assertEquals(ValidationResponse.ResultEnum.VALID, result.getData().getResult());
    }

    @Test
    void shouldRejectBothOfficerDeliveryStatementsTogether() {
        final var result = validate("WU02",
            List.of("personal-code", "name", "date-of-birth"),
            List.of("officer-employee-statement", "delivery-on-behalf-of-firm",
                "delivery-on-behalf-of-individual", "verified-statement"));

        assertUnprocessableReason(result, "$.statement_ids", "Statements do not match for form WU02.");
    }

    @Test
    void shouldRejectBothAcspDeliveryAlternativesTogether() {
        final var result = validate("MR01",
            List.of("acsp-code", "acsp-name", "email", "name"),
            List.of("acsp-statement", "delivery-on-behalf-of-firm", "delivery-on-behalf-of-individual"));

        assertUnprocessableReason(result, "$.statement_ids", "Statements do not match for form MR01.");
    }

    @Test
    void shouldRejectDataTypesThatDoNotMatchThePermittedPresenter() {
        final var result = validate("DS01",
            List.of("personal-code", "name", "date-of-birth"),
            List.of("verified-statement"));

        assertUnprocessableReason(result, "$.data_types", "Data types do not match for form DS01.");
    }

    private ServiceResult<ValidationResponse> validate(
        final String formType, final List<String> dataTypes, final List<String> statementIds) {
        final var request = new ValidationRequest();
        request.setFormType(formType);
        request.setDataTypes(dataTypes);
        request.setStatementIds(statementIds);

        return presenterValidationService.validatePresenterDetails(request);
    }

    private static void assertUnprocessableReason(
        final ServiceResult<ValidationResponse> result, final String location, final String reason) {
        assertEquals(ServiceResultStatus.UNPROCESSABLE, result.getStatus());
        assertTrue(result.getErrors().containsError(
            Err.invalidBodyBuilderWithLocation(location).withError(reason).build()));
    }
}

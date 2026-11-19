package uk.gov.companieshouse.presentersapi.service.impl;

import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.presenters.model.ValidationRequest;
import uk.gov.companieshouse.api.presenters.model.ValidationResponse;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.guard.RequestListGuard;
import uk.gov.companieshouse.presentersapi.model.domain.PresenterStatements;
import uk.gov.companieshouse.presentersapi.service.DataObjectTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.PresenterTypeMatrixService;
import uk.gov.companieshouse.presentersapi.service.PresenterValidationService;
import uk.gov.companieshouse.presentersapi.service.StatementTypeMatrixService;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;

@Service
public class PresenterValidationServiceImpl implements PresenterValidationService {

    private final PresenterTypeMatrixService presenterTypeMatrixService;
    private final DataObjectTypeMatrixService dataObjectTypeMatrixService;
    private final StatementTypeMatrixService statementTypeMatrixService;

    public PresenterValidationServiceImpl(final PresenterTypeMatrixService presenterTypeMatrixService,
        final DataObjectTypeMatrixService dataObjectTypeMatrixService,
        final StatementTypeMatrixService statementTypeMatrixService) {
        this.presenterTypeMatrixService = presenterTypeMatrixService;
        this.dataObjectTypeMatrixService = dataObjectTypeMatrixService;
        this.statementTypeMatrixService = statementTypeMatrixService;
    }

    /**
     * Validates exact data and statement requirements, requiring the same permitted presenter to match both.
     *
     * @param validationRequest the request already checked by the controller's request guards
     * @return a found result containing a VALID response when a presenter matches, or an unprocessable result
     *     containing errors when no presenters are permitted or no candidate matches the data or statements
     */
    @Override
    public ServiceResult<ValidationResponse> validatePresenterDetails(final ValidationRequest validationRequest) {
        final var formType = validatedFormType(validationRequest);
        final var submittedDataTypes = RequestListGuard.guardDataObjectTypes(validationRequest.getDataTypes()).value();
        final var submittedStatements = RequestListGuard.guardStatements(validationRequest.getStatementIds()).value();
        final var permittedPresenterTypes = presenterTypeMatrixService.presenterTypesAllowedFor(formType);

        if (permittedPresenterTypes.isEmpty()) {
            return noPermittedPresentersResult(formType);
        }

        final var dataMatchingPresenters = presentersMatchingDataTypes(permittedPresenterTypes, submittedDataTypes);
        if (dataMatchingPresenters.isEmpty()) {

            return nonValidResult("data_types", "Data types do not match for form %s.".formatted(formType.code()));
        }

        final var statementMatchingPresenters = presentersMatchingStatements(dataMatchingPresenters,
                                                                             formType,
                                                                             submittedStatements);
        if (statementMatchingPresenters.isEmpty()) {
            return nonValidResult("statement_ids", "Statements do not match for form %s.".formatted(formType.code()));
        }

        return validResult();
    }

    /**
     * Resolves the form type defensively if a caller bypasses request pre-validation.
     *
     * @throws IllegalStateException if the supplied form type is not recognised
     */
    private static FormType validatedFormType(final ValidationRequest validationRequest) {
        return FormType
                   .find(validationRequest.getFormType())
                   .orElseThrow(() -> new IllegalStateException(
                       "Form type must be validated before presenter validation"));
    }

    /**
     * Returns a defensive error when no presenters are permitted, for example if a caller bypasses
     * pre-validation and supplies an unsupported form. Validated configuration provides presenter mappings
     * for forms supported by this endpoint.
     */
    private ServiceResult<ValidationResponse> noPermittedPresentersResult(final FormType formType) {
        final var error = Err
                              .serviceErrBuilder()
                              .withError("No candidate presenter type supports the supplied form type: %s".formatted(
                                  formType.code()))
                              .build();
        return ServiceResult.unprocessable(new Errors(error));
    }

    /**
     * Keeps candidates whose complete mapped data-object set equals the submitted set.
     */
    private EnumSet<PresenterType> presentersMatchingDataTypes(final Set<PresenterType> candidates,
        final Set<DataObjectType> submittedDataTypes) {
        final var matches = EnumSet.noneOf(PresenterType.class);
        for (final var presenterType : candidates) {
            if (dataObjectTypeMatrixService.dataObjectTypesAllowedFor(presenterType).equals(submittedDataTypes)) {
                matches.add(presenterType);
            }
        }
        return matches;
    }

    /**
     * Keeps candidates whose submitted statements equal one complete valid combination.
     */
    private EnumSet<PresenterType> presentersMatchingStatements(final Set<PresenterType> candidates,
        final FormType formType, final PresenterStatements submittedStatements) {
        final var matches = EnumSet.noneOf(PresenterType.class);
        for (final var presenterType : candidates) {
            if (statementTypeMatrixService
                    .validStatementCombinationsFor(presenterType, formType)
                    .contains(submittedStatements)) {
                matches.add(presenterType);
            }
        }
        return matches;
    }

    private @NonNull ServiceResult<ValidationResponse> nonValidResult(final String location, final String reason) {
        final var error = Err.invalidBodyBuilderWithLocation(location).withError(reason).build();
        return ServiceResult.unprocessable(new Errors(error));
    }

    private @NonNull ServiceResult<ValidationResponse> validResult() {
        return ServiceResult.found(new ValidationResponse(ValidationResponse.ResultEnum.VALID));
    }

}

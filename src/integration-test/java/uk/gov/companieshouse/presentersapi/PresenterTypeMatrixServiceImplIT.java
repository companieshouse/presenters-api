package uk.gov.companieshouse.presentersapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.service.impl.PresenterTypeMatrixServiceImpl;

/**
 * Verifies PresenterTypeMatrixServiceImpl is wired with the BiMap beans populated from the reference data YAML files.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PresenterTypeMatrixServiceImplIT {

    @Autowired
    private PresenterTypeMatrixServiceImpl service;

    @Test
    void shouldResolvePresenterTypesForFormType() {
        assertThat(service.presenterTypesFor(FormType.AD01),
                containsInAnyOrder(PresenterType.OFFICER_EMPLOYEE, PresenterType.CORPORATE_OFFICER_EMPLOYEE,
                        PresenterType.EXEMPT_PRESENTER, PresenterType.ACSP_SOLE_TRADER, PresenterType.ACSP_EMPLOYEE));
        assertThat(service.presenterTypesFor(FormType.WU15), is(EnumSet.of(PresenterType.EXEMPT_PRESENTER)));
    }

    @Test
    void shouldReturnEmptySetForFormTypeWithoutFormGroup() {
        assertThat(service.presenterTypesFor(null), is(empty()));
    }
}

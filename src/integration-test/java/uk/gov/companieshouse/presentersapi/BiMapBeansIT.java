package uk.gov.companieshouse.presentersapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.presentersapi.data.ReferenceBiMapSummaryLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupDeliveryStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupFormTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.FormGroupPresenterTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeAssociationStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeDataObjectTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeDeliveryStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.bimaps.PresenterTypeVerificationStatementTypeBiMap;
import uk.gov.companieshouse.presentersapi.data.enums.AssociationStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.DataObjectType;
import uk.gov.companieshouse.presentersapi.data.enums.DeliveryStatementType;
import uk.gov.companieshouse.presentersapi.data.enums.FormGroup;
import uk.gov.companieshouse.presentersapi.data.enums.FormType;
import uk.gov.companieshouse.presentersapi.data.enums.PresenterType;
import uk.gov.companieshouse.presentersapi.data.enums.VerificationStatementType;

/**
 * Verifies the generated BiMap beans are created by @ConfigurationPropertiesScan and populated
 * from the reference data YAML files imported via spring.config.import.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "internal.api.url=http://localhost")
class BiMapBeansIT {

    @Autowired
    private ReferenceBiMapSummaryLogger summaryLogger;
    @Autowired
    private FormGroupFormTypeBiMap formGroupFormType;
    @Autowired
    private FormGroupPresenterTypeBiMap formGroupPresenterType;
    @Autowired
    private FormGroupDeliveryStatementTypeBiMap formGroupDeliveryStatement;
    @Autowired
    private PresenterTypeDeliveryStatementTypeBiMap presenterTypeDeliveryStatement;
    @Autowired
    private PresenterTypeVerificationStatementTypeBiMap presenterTypeVerificationStatement;
    @Autowired
    private PresenterTypeAssociationStatementTypeBiMap presenterTypeAssociationStatement;
    @Autowired
    private PresenterTypeDataObjectTypeBiMap presenterTypeDataObject;

    @Test
    void shouldPopulateFormGroupFormTypeInBothDirectionsWhenContextStarts() {
        assertThat(formGroupFormType.formTypesFor(FormGroup.from("firm-delivery")),
            containsInAnyOrder(FormType.from("AD01"), FormType.from("CS01")));
        assertThat(formGroupFormType.formTypesFor(FormGroup.from("official-receiver")), is(empty()));
        assertThat(formGroupFormType.formGroupFor(FormType.from("PSC01")), is(FormGroup.from("individual-delivery")));
    }

    @Test
    void shouldPopulateFormGroupPresenterTypeInBothDirectionsWhenContextStarts() {
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.from("acsp-only-firm-delivery")),
            containsInAnyOrder(PresenterType.from("acsp-sole-trader"), PresenterType.from("acsp-employee")));
        assertThat(formGroupPresenterType.formGroupsFor(PresenterType.from("exempt-presenter")),
            containsInAnyOrder(FormGroup.from("firm-delivery"), FormGroup.from("individual-delivery"),
                FormGroup.from("multi-delivery"), FormGroup.from("official-receiver"), FormGroup.from("exempt-form")));
    }

    @Test
    void shouldPopulateFormGroupDeliveryStatementTypeInBothDirectionsWhenContextStarts() {
        final var firm = DeliveryStatementType.from("delivery-on-behalf-of-firm");
        final var individual = DeliveryStatementType.from("delivery-on-behalf-of-individual");

        assertThat(formGroupDeliveryStatement.deliveryStatementTypesFor(FormGroup.from("multi-delivery")),
            containsInAnyOrder(firm, individual));
        assertThat(formGroupDeliveryStatement.formGroupsFor(firm),
            containsInAnyOrder(FormGroup.from("firm-delivery"), FormGroup.from("multi-delivery")));
    }

    @Test
    void shouldPopulatePresenterTypeDeliveryStatementTypeInBothDirectionsWhenContextStarts() {
        final var firm = DeliveryStatementType.from("delivery-on-behalf-of-firm");

        assertThat(presenterTypeDeliveryStatement.deliveryStatementTypesFor(PresenterType.from("individual-filing-for-self")),
            is(empty()));
        assertThat(presenterTypeDeliveryStatement.presenterTypesFor(firm),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee")));
    }

    @Test
    void shouldPopulatePresenterTypeVerificationStatementTypeInBothDirectionsWhenContextStarts() {
        final var verified = VerificationStatementType.from("verified-statement");

        assertThat(presenterTypeVerificationStatement.verificationStatementTypesFor(PresenterType.from("acsp-employee")),
            is(empty()));
        assertThat(presenterTypeVerificationStatement.presenterTypesFor(verified),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee"),
                PresenterType.from("individual-filing-for-self"), PresenterType.from("individual-filing-for-someone-else")));
    }

    @Test
    void shouldPopulatePresenterTypeAssociationStatementTypeInBothDirectionsWhenContextStarts() {
        final var soleTrader = AssociationStatementType.from("acsp-sole-trader-statement");

        assertThat(presenterTypeAssociationStatement.associationStatementTypesFor(PresenterType.from("exempt-presenter")),
            containsInAnyOrder(AssociationStatementType.from("exempt-statement")));
        assertThat(presenterTypeAssociationStatement.presenterTypesFor(soleTrader),
            containsInAnyOrder(PresenterType.from("acsp-sole-trader"), PresenterType.from("acsp-employee")));
    }

    @Test
    void shouldPopulatePresenterTypeDataObjectTypeInBothDirectionsWhenContextStarts() {
        assertThat(presenterTypeDataObject.dataObjectTypesFor(PresenterType.from("individual-filing-for-self")),
            containsInAnyOrder(DataObjectType.from("personal-code"), DataObjectType.from("name")));
        assertThat(presenterTypeDataObject.presenterTypesFor(DataObjectType.from("date-of-birth")),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee"),
                PresenterType.from("individual-filing-for-someone-else")));
    }

    @Test
    void shouldNotExposeInternalSetsWhenCallersMutateReturnedSets() {
        final var firmDelivery = FormGroup.from("firm-delivery");
        final var exemptPresenter = PresenterType.from("exempt-presenter");

        formGroupFormType.formTypesFor(firmDelivery).clear();
        formGroupPresenterType.presenterTypesFor(firmDelivery).clear();
        formGroupPresenterType.formGroupsFor(exemptPresenter).clear();

        assertThat(formGroupFormType.formTypesFor(firmDelivery),
            containsInAnyOrder(FormType.from("AD01"), FormType.from("CS01")));
        assertThat(formGroupPresenterType.presenterTypesFor(firmDelivery).isEmpty(), is(false));
        assertThat(formGroupPresenterType.formGroupsFor(exemptPresenter).isEmpty(), is(false));
    }

    @Test
    void shouldReportEveryBiMapWithPopulatedEntryCountsWhenContextStarts() {
        final var lines = summaryLogger.summaryLines();

        assertThat(lines.size(), is(7));
        assertThat(lines.contains("BiMap 'FormGroupFormTypeBiMap' created: 8 forward mappings (keys), 8 reverse mappings (values)"), is(true));
    }
}

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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
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
        assertThat(formGroupFormType.formTypesFor(FormGroup.from("official-receiver-forms")), is(empty()));
        assertThat(formGroupFormType.formGroupFor(FormType.from("PSC01")), is(FormGroup.from("individual-delivery")));
    }

    @Test
    void shouldPopulateFormGroupPresenterTypeInBothDirectionsWhenContextStarts() {
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.from("acsp-only-firm-delivery")),
            containsInAnyOrder(PresenterType.from("acsp")));
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.from("exempt-form")),
            containsInAnyOrder(PresenterType.from("official-receiver")));
        assertThat(formGroupPresenterType.formGroupsFor(PresenterType.from("official-receiver")),
            containsInAnyOrder(FormGroup.from("official-receiver-forms"), FormGroup.from("exempt-form")));
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.from("official-receiver-forms")),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee"),
                PresenterType.from("judicial-factor"), PresenterType.from("officer-employee-same-group"),
                PresenterType.from("acsp"), PresenterType.from("official-receiver")));
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
        assertThat(presenterTypeDeliveryStatement.deliveryStatementTypesFor(PresenterType.from("judicial-factor")),
            containsInAnyOrder(firm));
        assertThat(presenterTypeDeliveryStatement.deliveryStatementTypesFor(
            PresenterType.from("officer-employee-same-group")), containsInAnyOrder(firm));
        assertThat(presenterTypeDeliveryStatement.presenterTypesFor(firm),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee"),
                PresenterType.from("judicial-factor"), PresenterType.from("officer-employee-same-group")));
    }

    @Test
    void shouldPopulatePresenterTypeVerificationStatementTypeInBothDirectionsWhenContextStarts() {
        final var verified = VerificationStatementType.from("verified-statement");

        assertThat(presenterTypeVerificationStatement.verificationStatementTypesFor(PresenterType.from("acsp")),
            is(empty()));
        assertThat(presenterTypeVerificationStatement.presenterTypesFor(verified),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee"),
                PresenterType.from("judicial-factor"), PresenterType.from("officer-employee-same-group"),
                PresenterType.from("individual-filing-for-self"), PresenterType.from("individual-filing-for-someone-else")));
    }

    @Test
    void shouldPopulatePresenterTypeAssociationStatementTypeInBothDirectionsWhenContextStarts() {
        final var acsp = AssociationStatementType.from("acsp-statement");

        assertThat(presenterTypeAssociationStatement.associationStatementTypesFor(PresenterType.from("acsp")),
            containsInAnyOrder(acsp));
        assertThat(presenterTypeAssociationStatement.presenterTypesFor(acsp),
            containsInAnyOrder(PresenterType.from("acsp")));
        assertThat(presenterTypeAssociationStatement.associationStatementTypesFor(PresenterType.from("judicial-factor")),
            containsInAnyOrder(AssociationStatementType.from("judicial-factor-statement")));
    }

    @Test
    void shouldPopulatePresenterTypeDataObjectTypeInBothDirectionsWhenContextStarts() {
        assertThat(presenterTypeDataObject.dataObjectTypesFor(PresenterType.from("individual-filing-for-self")),
            containsInAnyOrder(DataObjectType.from("personal-code"), DataObjectType.from("name")));
        assertThat(presenterTypeDataObject.dataObjectTypesFor(PresenterType.from("official-receiver")),
            containsInAnyOrder(DataObjectType.from("organisation-name"), DataObjectType.from("name"),
                DataObjectType.from("email")));
        assertThat(presenterTypeDataObject.presenterTypesFor(DataObjectType.from("date-of-birth")),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee"),
                PresenterType.from("judicial-factor"), PresenterType.from("officer-employee-same-group"),
                PresenterType.from("individual-filing-for-someone-else")));
    }

    @Test
    void shouldNotExposeInternalSetsWhenCallersMutateReturnedSets() {
        final var firmDelivery = FormGroup.from("firm-delivery");
        final var officialReceiver = PresenterType.from("official-receiver");

        formGroupFormType.formTypesFor(firmDelivery).clear();
        formGroupPresenterType.presenterTypesFor(firmDelivery).clear();
        formGroupPresenterType.formGroupsFor(officialReceiver).clear();

        assertThat(formGroupFormType.formTypesFor(firmDelivery),
            containsInAnyOrder(FormType.from("AD01"), FormType.from("CS01")));
        assertThat(formGroupPresenterType.presenterTypesFor(firmDelivery).isEmpty(), is(false));
        assertThat(formGroupPresenterType.formGroupsFor(officialReceiver),
            containsInAnyOrder(FormGroup.from("official-receiver-forms"), FormGroup.from("exempt-form")));
    }

    @Test
    void shouldReportEveryBiMapWithPopulatedEntryCountsWhenContextStarts() {
        final var lines = summaryLogger.summaryLines();

        assertThat(lines.size(), is(7));
        assertThat(lines.contains("BiMap 'FormGroupFormTypeBiMap' created: 8 forward mappings (keys), 8 reverse mappings (values)"), is(true));
    }
}

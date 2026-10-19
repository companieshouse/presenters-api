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
        assertThat(formGroupFormType.formTypesFor(FormGroup.from("self-delivery")),
            containsInAnyOrder(FormType.from("DS01"), FormType.from("LL DS01"), FormType.from("ACSP01-Sole")));
        assertThat(formGroupFormType.formGroupFor(FormType.from("DS01")), is(FormGroup.from("self-delivery")));
        assertThat(formGroupFormType.formGroupFor(FormType.from("PSC01")), is(FormGroup.from("firm-delivery")));
    }

    @Test
    void shouldPopulateFormGroupPresenterTypeInBothDirectionsWhenContextStarts() {
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.from("acsp-only")),
            containsInAnyOrder(PresenterType.from("acsp")));
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.from("unmarked")),
            is(empty()));
        assertThat(formGroupPresenterType.formGroupsFor(PresenterType.from("official-receiver")), is(empty()));
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.from("wu-sub-forms")),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee"),
                PresenterType.from("judicial-factor"), PresenterType.from("officer-employee-same-group"),
                PresenterType.from("acsp"), PresenterType.from("individual-representing")));
    }

    @Test
    void shouldPopulateFormGroupDeliveryStatementTypeInBothDirectionsWhenContextStarts() {
        final var firm = DeliveryStatementType.from("delivery-on-behalf-of-firm");
        final var individual = DeliveryStatementType.from("delivery-on-behalf-of-individual");

        assertThat(formGroupDeliveryStatement.deliveryStatementTypesFor(FormGroup.from("multi-delivery")),
            containsInAnyOrder(firm, individual));
        assertThat(formGroupDeliveryStatement.formGroupsFor(firm),
            containsInAnyOrder(FormGroup.from("firm-delivery"), FormGroup.from("multi-delivery"),
                FormGroup.from("officer-only"), FormGroup.from("wu-sub-forms")));
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
                PresenterType.from("individual-filing-for-self"), PresenterType.from("individual-representing")));
        assertThat(presenterTypeVerificationStatement.verificationStatementTypesFor(PresenterType.from("official-receiver")),
            is(empty()));
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
            containsInAnyOrder(DataObjectType.from("email")));
        assertThat(presenterTypeDataObject.presenterTypesFor(DataObjectType.from("date-of-birth")),
            containsInAnyOrder(PresenterType.from("officer-employee"), PresenterType.from("corporate-officer-employee"),
                PresenterType.from("judicial-factor"), PresenterType.from("officer-employee-same-group"),
                PresenterType.from("individual-representing")));
    }

    @Test
    void shouldNotExposeInternalSetsWhenCallersMutateReturnedSets() {
        final var selfDelivery = FormGroup.from("self-delivery");
        final var officer = PresenterType.from("officer-employee");

        formGroupFormType.formTypesFor(selfDelivery).clear();
        formGroupPresenterType.presenterTypesFor(selfDelivery).clear();
        formGroupPresenterType.formGroupsFor(officer).clear();

        assertThat(formGroupFormType.formTypesFor(selfDelivery),
            containsInAnyOrder(FormType.from("DS01"), FormType.from("LL DS01"), FormType.from("ACSP01-Sole")));
        assertThat(formGroupPresenterType.presenterTypesFor(selfDelivery),
            containsInAnyOrder(PresenterType.from("individual-filing-for-self")));
        assertThat(formGroupPresenterType.formGroupsFor(officer),
            containsInAnyOrder(FormGroup.from("firm-delivery"), FormGroup.from("multi-delivery"),
                FormGroup.from("officer-only"), FormGroup.from("wu-sub-forms")));
    }

    @Test
    void shouldReportEveryBiMapWithPopulatedEntryCountsWhenContextStarts() {
        final var lines = summaryLogger.summaryLines();

        assertThat(lines.size(), is(7));
        assertThat(lines.contains("BiMap 'FormGroupFormTypeBiMap' created: " + FormGroup.values().length
            + " forward mappings (keys), " + FormType.values().length + " reverse mappings (values)"), is(true));
    }
}

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
        assertThat(formGroupFormType.formTypesFor(FormGroup.find("self-delivery").orElseThrow()),
            containsInAnyOrder(FormType.find("DS01").orElseThrow(), FormType.find("LL DS01").orElseThrow(), FormType.find("ACSP01-Sole").orElseThrow()));
        assertThat(formGroupFormType.formGroupFor(FormType.find("DS01").orElseThrow()), is(FormGroup.find("self-delivery").orElseThrow()));
        assertThat(formGroupFormType.formGroupFor(FormType.find("PSC01").orElseThrow()), is(FormGroup.find("firm-delivery").orElseThrow()));
    }

    @Test
    void shouldPopulateFormGroupPresenterTypeInBothDirectionsWhenContextStarts() {
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.find("acsp-only").orElseThrow()),
            containsInAnyOrder(PresenterType.find("acsp").orElseThrow()));
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.find("unmarked").orElseThrow()),
            is(empty()));
        assertThat(formGroupPresenterType.formGroupsFor(PresenterType.find("official-receiver").orElseThrow()), is(empty()));
        assertThat(formGroupPresenterType.presenterTypesFor(FormGroup.find("wu-sub-forms").orElseThrow()),
            containsInAnyOrder(PresenterType.find("officer-employee").orElseThrow(), PresenterType.find("corporate-officer-employee").orElseThrow(),
                PresenterType.find("judicial-factor").orElseThrow(), PresenterType.find("officer-employee-same-group").orElseThrow(),
                PresenterType.find("acsp").orElseThrow(), PresenterType.find("individual-representing").orElseThrow()));
    }

    @Test
    void shouldPopulateFormGroupDeliveryStatementTypeInBothDirectionsWhenContextStarts() {
        final var firm = DeliveryStatementType.find("delivery-on-behalf-of-firm").orElseThrow();
        final var individual = DeliveryStatementType.find("delivery-on-behalf-of-individual").orElseThrow();

        assertThat(formGroupDeliveryStatement.deliveryStatementTypesFor(FormGroup.find("multi-delivery").orElseThrow()),
            containsInAnyOrder(firm, individual));
        assertThat(formGroupDeliveryStatement.formGroupsFor(firm),
            containsInAnyOrder(FormGroup.find("firm-delivery").orElseThrow(), FormGroup.find("multi-delivery").orElseThrow(),
                FormGroup.find("officer-only").orElseThrow(), FormGroup.find("wu-sub-forms").orElseThrow()));
    }

    @Test
    void shouldPopulatePresenterTypeDeliveryStatementTypeInBothDirectionsWhenContextStarts() {
        final var firm = DeliveryStatementType.find("delivery-on-behalf-of-firm").orElseThrow();

        assertThat(presenterTypeDeliveryStatement.deliveryStatementTypesFor(PresenterType.find("individual-filing-for-self").orElseThrow()),
            is(empty()));
        assertThat(presenterTypeDeliveryStatement.deliveryStatementTypesFor(PresenterType.find("judicial-factor").orElseThrow()),
            containsInAnyOrder(firm));
        assertThat(presenterTypeDeliveryStatement.deliveryStatementTypesFor(
            PresenterType.find("officer-employee-same-group").orElseThrow()), containsInAnyOrder(firm));
        assertThat(presenterTypeDeliveryStatement.presenterTypesFor(firm),
            containsInAnyOrder(PresenterType.find("officer-employee").orElseThrow(), PresenterType.find("corporate-officer-employee").orElseThrow(),
                PresenterType.find("judicial-factor").orElseThrow(), PresenterType.find("officer-employee-same-group").orElseThrow()));
    }

    @Test
    void shouldPopulatePresenterTypeVerificationStatementTypeInBothDirectionsWhenContextStarts() {
        final var verified = VerificationStatementType.find("verified-statement").orElseThrow();

        assertThat(presenterTypeVerificationStatement.verificationStatementTypesFor(PresenterType.find("acsp").orElseThrow()),
            is(empty()));
        assertThat(presenterTypeVerificationStatement.presenterTypesFor(verified),
            containsInAnyOrder(PresenterType.find("officer-employee").orElseThrow(), PresenterType.find("corporate-officer-employee").orElseThrow(),
                PresenterType.find("judicial-factor").orElseThrow(), PresenterType.find("officer-employee-same-group").orElseThrow(),
                PresenterType.find("individual-filing-for-self").orElseThrow(), PresenterType.find("individual-representing").orElseThrow()));
        assertThat(presenterTypeVerificationStatement.verificationStatementTypesFor(PresenterType.find("official-receiver").orElseThrow()),
            is(empty()));
    }

    @Test
    void shouldPopulatePresenterTypeAssociationStatementTypeInBothDirectionsWhenContextStarts() {
        final var acsp = AssociationStatementType.find("acsp-statement").orElseThrow();

        assertThat(presenterTypeAssociationStatement.associationStatementTypesFor(PresenterType.find("acsp").orElseThrow()),
            containsInAnyOrder(acsp));
        assertThat(presenterTypeAssociationStatement.presenterTypesFor(acsp),
            containsInAnyOrder(PresenterType.find("acsp").orElseThrow()));
        assertThat(presenterTypeAssociationStatement.associationStatementTypesFor(PresenterType.find("judicial-factor").orElseThrow()),
            containsInAnyOrder(AssociationStatementType.find("judicial-factor-statement").orElseThrow()));
    }

    @Test
    void shouldPopulatePresenterTypeDataObjectTypeInBothDirectionsWhenContextStarts() {
        assertThat(presenterTypeDataObject.dataObjectTypesFor(PresenterType.find("individual-filing-for-self").orElseThrow()),
            containsInAnyOrder(DataObjectType.find("personal-code").orElseThrow(), DataObjectType.find("name").orElseThrow()));
        assertThat(presenterTypeDataObject.dataObjectTypesFor(PresenterType.find("official-receiver").orElseThrow()),
            containsInAnyOrder(DataObjectType.find("email").orElseThrow()));
        assertThat(presenterTypeDataObject.presenterTypesFor(DataObjectType.find("date-of-birth").orElseThrow()),
            containsInAnyOrder(PresenterType.find("officer-employee").orElseThrow(), PresenterType.find("corporate-officer-employee").orElseThrow(),
                PresenterType.find("judicial-factor").orElseThrow(), PresenterType.find("officer-employee-same-group").orElseThrow(),
                PresenterType.find("individual-representing").orElseThrow()));
    }

    @Test
    void shouldNotExposeInternalSetsWhenCallersMutateReturnedSets() {
        final var selfDelivery = FormGroup.find("self-delivery").orElseThrow();
        final var officer = PresenterType.find("officer-employee").orElseThrow();

        formGroupFormType.formTypesFor(selfDelivery).clear();
        formGroupPresenterType.presenterTypesFor(selfDelivery).clear();
        formGroupPresenterType.formGroupsFor(officer).clear();

        assertThat(formGroupFormType.formTypesFor(selfDelivery),
            containsInAnyOrder(FormType.find("DS01").orElseThrow(), FormType.find("LL DS01").orElseThrow(), FormType.find("ACSP01-Sole").orElseThrow()));
        assertThat(formGroupPresenterType.presenterTypesFor(selfDelivery),
            containsInAnyOrder(PresenterType.find("individual-filing-for-self").orElseThrow()));
        assertThat(formGroupPresenterType.formGroupsFor(officer),
            containsInAnyOrder(FormGroup.find("firm-delivery").orElseThrow(), FormGroup.find("multi-delivery").orElseThrow(),
                FormGroup.find("officer-only").orElseThrow(), FormGroup.find("wu-sub-forms").orElseThrow()));
    }

    @Test
    void shouldReportEveryBiMapWithPopulatedEntryCountsWhenContextStarts() {
        final var lines = summaryLogger.summaryLines();

        assertThat(lines.size(), is(7));
        assertThat(lines.contains("BiMap 'FormGroupFormTypeBiMap' created: " + FormGroup.values().length
            + " forward mappings (keys), " + FormType.values().length + " reverse mappings (values)"), is(true));
    }
}

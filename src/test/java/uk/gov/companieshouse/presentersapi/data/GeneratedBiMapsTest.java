package uk.gov.companieshouse.presentersapi.data;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
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
 * Unit tests for the generated BiMap constructors, covering the paths the Spring-bound data never takes:
 * no bound map, an empty value set, and the VALUES_PER_KEY_SET duplicate-value check.
 * Relies on the BiMaps produced from enum-generator-config.yaml.
 */
class GeneratedBiMapsTest {

    private static final FormGroup GROUP_1 = FormGroup.values()[0];
    private static final FormGroup GROUP_2 = FormGroup.values()[1];
    private static final PresenterType PRESENTER = PresenterType.values()[0];

    // ── VALUES_PER_KEY_SET (singular reverse mapping) ──

    @Test
    void shouldReturnEmptyLookupsWhenFormGroupFormTypeHasNoBoundMap() {
        final var biMap = new FormGroupFormTypeBiMap(null);

        assertThat(biMap.formTypesFor(GROUP_1), empty());
        assertThat(biMap.formGroupFor(FormType.values()[0]), is(nullValue()));
    }

    @Test
    void shouldReturnNoFormTypesWhenFormGroupHasEmptySet() {
        final var biMap = new FormGroupFormTypeBiMap(Map.of(GROUP_1, Set.of()));

        assertThat(biMap.formTypesFor(GROUP_1), empty());
    }

    @Test
    void shouldRejectFormTypeWhenMappedUnderTwoFormGroups() {
        final var form = FormType.values()[0];
        final var data = new LinkedHashMap<FormGroup, Set<FormType>>();
        data.put(GROUP_1, EnumSet.of(form));
        data.put(GROUP_2, EnumSet.of(form));

        final var exception = assertThrows(IllegalStateException.class, () -> new FormGroupFormTypeBiMap(data));

        assertThat(exception.getMessage(), containsString("mapped by both"));
    }

    @Test
    void shouldAcceptDistinctFormTypesWhenUnderDifferentFormGroups() {
        final var forms = FormType.values();
        final var data = new LinkedHashMap<FormGroup, Set<FormType>>();
        data.put(GROUP_1, EnumSet.of(forms[0]));
        data.put(GROUP_2, EnumSet.of(forms[1]));

        final var biMap = new FormGroupFormTypeBiMap(data);

        assertThat(biMap.formGroupFor(forms[1]), org.hamcrest.CoreMatchers.is(GROUP_2));
    }

    // ── VALUES_PER_KEY (plural reverse mapping) ──

    @Test
    void shouldReturnEmptyResultsWhenFormGroupPresenterTypeMapIsNullOrEmpty() {
        assertThat(new FormGroupPresenterTypeBiMap(null).presenterTypesFor(GROUP_1), empty());
        final var withEmpty = new FormGroupPresenterTypeBiMap(Map.of(GROUP_1, Set.of()));
        assertThat(withEmpty.presenterTypesFor(GROUP_1), empty());
        assertThat(withEmpty.formGroupsFor(PRESENTER), empty());
    }

    @Test
    void shouldMapPresenterTypeToEveryFormGroupWhenValueIsSharedAcrossKeys() {
        final var data = new LinkedHashMap<FormGroup, Set<PresenterType>>();
        data.put(GROUP_1, EnumSet.of(PRESENTER));
        data.put(GROUP_2, EnumSet.of(PRESENTER));

        final var biMap = new FormGroupPresenterTypeBiMap(data);

        assertThat(biMap.formGroupsFor(PRESENTER), contains(GROUP_1, GROUP_2));
    }

    @Test
    void shouldReturnEmptyResultsWhenFormGroupDeliveryStatementTypeMapIsNullOrEmpty() {
        assertThat(new FormGroupDeliveryStatementTypeBiMap(null).deliveryStatementTypesFor(GROUP_1), empty());
        final var withEmpty = new FormGroupDeliveryStatementTypeBiMap(Map.of(GROUP_1, Set.of()));
        assertThat(withEmpty.deliveryStatementTypesFor(GROUP_1), empty());
        assertThat(withEmpty.formGroupsFor(DeliveryStatementType.values()[0]), empty());
    }

    @Test
    void shouldReturnEmptyResultsWhenPresenterTypeDeliveryStatementTypeMapIsNullOrEmpty() {
        assertThat(new PresenterTypeDeliveryStatementTypeBiMap(null).deliveryStatementTypesFor(PRESENTER), empty());
        final var withEmpty = new PresenterTypeDeliveryStatementTypeBiMap(Map.of(PRESENTER, Set.of()));
        assertThat(withEmpty.deliveryStatementTypesFor(PRESENTER), empty());
        assertThat(withEmpty.presenterTypesFor(DeliveryStatementType.values()[0]), empty());
    }

    @Test
    void shouldReturnEmptyResultsWhenPresenterTypeVerificationStatementTypeMapIsNullOrEmpty() {
        assertThat(new PresenterTypeVerificationStatementTypeBiMap(null).verificationStatementTypesFor(PRESENTER), empty());
        final var withEmpty = new PresenterTypeVerificationStatementTypeBiMap(Map.of(PRESENTER, Set.of()));
        assertThat(withEmpty.verificationStatementTypesFor(PRESENTER), empty());
        assertThat(withEmpty.presenterTypesFor(VerificationStatementType.values()[0]), empty());
    }

    @Test
    void shouldReturnEmptyResultsWhenPresenterTypeAssociationStatementTypeMapIsNullOrEmpty() {
        assertThat(new PresenterTypeAssociationStatementTypeBiMap(null).associationStatementTypesFor(PRESENTER), empty());
        final var withEmpty = new PresenterTypeAssociationStatementTypeBiMap(Map.of(PRESENTER, Set.of()));
        assertThat(withEmpty.associationStatementTypesFor(PRESENTER), empty());
        assertThat(withEmpty.presenterTypesFor(AssociationStatementType.values()[0]), empty());
    }

    @Test
    void shouldReturnEmptyResultsWhenPresenterTypeDataObjectTypeMapIsNullOrEmpty() {
        assertThat(new PresenterTypeDataObjectTypeBiMap(null).dataObjectTypesFor(PRESENTER), empty());
        final var withEmpty = new PresenterTypeDataObjectTypeBiMap(Map.of(PRESENTER, Set.of()));
        assertThat(withEmpty.dataObjectTypesFor(PRESENTER), empty());
        assertThat(withEmpty.presenterTypesFor(DataObjectType.values()[0]), empty());
    }
}

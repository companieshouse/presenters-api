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
 * Unit tests for generated BiMap lookups, entry counts, and constructor edge cases.
 * Relies on the BiMaps produced from enum-generator-config.yaml.
 */
class GeneratedBiMapsTest {

    private static final FormGroup GROUP_1 = FormGroup.values()[0];
    private static final FormGroup GROUP_2 = FormGroup.values()[1];
    private static final PresenterType PRESENTER = PresenterType.values()[0];

    private final FormGroupFormTypeBiMap unboundFormGroupFormTypeBiMap = new FormGroupFormTypeBiMap(null);
    private final FormGroupPresenterTypeBiMap unboundFormGroupPresenterTypeBiMap = new FormGroupPresenterTypeBiMap(null);
    private final FormGroupDeliveryStatementTypeBiMap unboundFormGroupDeliveryStatementTypeBiMap =
        new FormGroupDeliveryStatementTypeBiMap(null);
    private final PresenterTypeDeliveryStatementTypeBiMap unboundPresenterTypeDeliveryStatementTypeBiMap =
        new PresenterTypeDeliveryStatementTypeBiMap(null);
    private final PresenterTypeVerificationStatementTypeBiMap unboundPresenterTypeVerificationStatementTypeBiMap =
        new PresenterTypeVerificationStatementTypeBiMap(null);
    private final PresenterTypeAssociationStatementTypeBiMap unboundPresenterTypeAssociationStatementTypeBiMap =
        new PresenterTypeAssociationStatementTypeBiMap(null);
    private final PresenterTypeDataObjectTypeBiMap unboundPresenterTypeDataObjectTypeBiMap =
        new PresenterTypeDataObjectTypeBiMap(null);

    // ── VALUES_PER_KEY_SET (singular reverse mapping) ──

    @Test
    void shouldReturnEmptyResultsForNonNullLookupsWhenFormGroupFormTypeHasNoBoundMap() {
        assertThat(unboundFormGroupFormTypeBiMap.formTypesFor(GROUP_1), empty());
        assertThat(unboundFormGroupFormTypeBiMap.formGroupFor(FormType.values()[0]), is(nullValue()));
    }

    @Test
    void shouldRejectNullFormTypeLookup() {
        assertThrows(NullPointerException.class, () -> unboundFormGroupFormTypeBiMap.formGroupFor(null));
    }

    @Test
    void shouldRejectNullKeysInForwardLookups() {
        assertThrows(NullPointerException.class, () -> unboundFormGroupFormTypeBiMap.formTypesFor(null));
        assertThrows(NullPointerException.class, () -> unboundFormGroupPresenterTypeBiMap.presenterTypesFor(null));
        assertThrows(NullPointerException.class, () -> unboundFormGroupDeliveryStatementTypeBiMap.deliveryStatementTypesFor(null));
        assertThrows(NullPointerException.class,
            () -> unboundPresenterTypeDeliveryStatementTypeBiMap.deliveryStatementTypesFor(null));
        assertThrows(NullPointerException.class,
            () -> unboundPresenterTypeVerificationStatementTypeBiMap.verificationStatementTypesFor(null));
        assertThrows(NullPointerException.class,
            () -> unboundPresenterTypeAssociationStatementTypeBiMap.associationStatementTypesFor(null));
        assertThrows(NullPointerException.class, () -> unboundPresenterTypeDataObjectTypeBiMap.dataObjectTypesFor(null));
    }

    @Test
    void shouldReturnNoFormTypesForFormGroupWithEmptySet() {
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

    @Test
    void shouldPopulateLookupsAndEntryCountsForFormGroupFormTypeBiMap() {
        final var forms = FormType.values();
        final var biMap = new FormGroupFormTypeBiMap(Map.of(
            GROUP_1, EnumSet.of(forms[0], forms[1]),
            GROUP_2, EnumSet.of(forms[2])
        ));
        assertThat(biMap.formTypesFor(GROUP_1), contains(forms[0], forms[1]));
        assertThat(biMap.formGroupFor(forms[2]), is(GROUP_2));
        assertEntryCounts(biMap, 2, 3);
    }

    @Test
    void shouldPopulateLookupsAndEntryCountsForFormGroupPresenterTypeBiMap() {
        final var biMap = new FormGroupPresenterTypeBiMap(valueUnderBothFormGroups(PRESENTER));
        assertThat(biMap.presenterTypesFor(GROUP_1), contains(PRESENTER));
        assertThat(biMap.formGroupsFor(PRESENTER), contains(GROUP_1, GROUP_2));
        assertEntryCounts(biMap, 2, 1);
    }

    @Test
    void shouldPopulateLookupsAndEntryCountsForFormGroupDeliveryStatementTypeBiMap() {
        final var deliveryStatement = DeliveryStatementType.values()[0];
        final var biMap = new FormGroupDeliveryStatementTypeBiMap(valueUnderBothFormGroups(deliveryStatement));
        assertThat(biMap.deliveryStatementTypesFor(GROUP_1), contains(deliveryStatement));
        assertThat(biMap.formGroupsFor(deliveryStatement), contains(GROUP_1, GROUP_2));
        assertEntryCounts(biMap, 2, 1);
    }

    @Test
    void shouldPopulateLookupsAndEntryCountsForPresenterTypeDeliveryStatementTypeBiMap() {
        final var deliveryStatement = DeliveryStatementType.values()[0];
        final var biMap = new PresenterTypeDeliveryStatementTypeBiMap(valueUnderPresenter(deliveryStatement));
        assertThat(biMap.deliveryStatementTypesFor(PRESENTER), contains(deliveryStatement));
        assertThat(biMap.presenterTypesFor(deliveryStatement), contains(PRESENTER));
        assertEntryCounts(biMap, 1, 1);
    }

    @Test
    void shouldPopulateLookupsAndEntryCountsForPresenterTypeVerificationStatementTypeBiMap() {
        final var verificationStatement = VerificationStatementType.values()[0];
        final var biMap = new PresenterTypeVerificationStatementTypeBiMap(valueUnderPresenter(verificationStatement));
        assertThat(biMap.verificationStatementTypesFor(PRESENTER), contains(verificationStatement));
        assertThat(biMap.presenterTypesFor(verificationStatement), contains(PRESENTER));
        assertEntryCounts(biMap, 1, 1);
    }

    @Test
    void shouldPopulateLookupsAndEntryCountsForPresenterTypeAssociationStatementTypeBiMap() {
        final var associationStatement = AssociationStatementType.values()[0];
        final var biMap = new PresenterTypeAssociationStatementTypeBiMap(valueUnderPresenter(associationStatement));
        assertThat(biMap.associationStatementTypesFor(PRESENTER), contains(associationStatement));
        assertThat(biMap.presenterTypesFor(associationStatement), contains(PRESENTER));
        assertEntryCounts(biMap, 1, 1);
    }

    @Test
    void shouldPopulateLookupsAndEntryCountsForPresenterTypeDataObjectTypeBiMap() {
        final var dataObject = DataObjectType.values()[0];
        final var biMap = new PresenterTypeDataObjectTypeBiMap(valueUnderPresenter(dataObject));
        assertThat(biMap.dataObjectTypesFor(PRESENTER), contains(dataObject));
        assertThat(biMap.presenterTypesFor(dataObject), contains(PRESENTER));
        assertEntryCounts(biMap, 1, 1);
    }

    private static <V extends Enum<V>> Map<FormGroup, Set<V>> valueUnderBothFormGroups(final V value) {
        return Map.of(GROUP_1, EnumSet.of(value), GROUP_2, EnumSet.of(value));
    }

    private static <V extends Enum<V>> Map<PresenterType, Set<V>> valueUnderPresenter(final V value) {
        return Map.of(PRESENTER, EnumSet.of(value));
    }

    private static void assertEntryCounts(
        final ReferenceBiMap biMap, final int expectedForwardCount, final int expectedReverseCount
    ) {
        assertThat("Forward entry count", biMap.forwardEntryCount(), is(expectedForwardCount));
        assertThat("Reverse entry count", biMap.reverseEntryCount(), is(expectedReverseCount));
    }

    // ── VALUES_PER_KEY (plural reverse mapping) ──

    @Test
    void shouldReturnEmptyResultsForNonNullLookupsWhenFormGroupPresenterTypeMapIsUnboundOrEmpty() {
        assertThat(unboundFormGroupPresenterTypeBiMap.presenterTypesFor(GROUP_1), empty());
        final var withEmpty = new FormGroupPresenterTypeBiMap(Map.of(GROUP_1, Set.of()));
        assertThat(withEmpty.presenterTypesFor(GROUP_1), empty());
        assertThat(withEmpty.formGroupsFor(PRESENTER), empty());
    }

    @Test
    void shouldRejectNullValuesInPluralReverseLookups() {
        assertThrows(NullPointerException.class, () -> unboundFormGroupPresenterTypeBiMap.formGroupsFor(null));
        assertThrows(NullPointerException.class, () -> unboundFormGroupDeliveryStatementTypeBiMap.formGroupsFor(null));
        assertThrows(NullPointerException.class,
            () -> unboundPresenterTypeDeliveryStatementTypeBiMap.presenterTypesFor(null));
        assertThrows(NullPointerException.class,
            () -> unboundPresenterTypeVerificationStatementTypeBiMap.presenterTypesFor(null));
        assertThrows(NullPointerException.class,
            () -> unboundPresenterTypeAssociationStatementTypeBiMap.presenterTypesFor(null));
        assertThrows(NullPointerException.class, () -> unboundPresenterTypeDataObjectTypeBiMap.presenterTypesFor(null));
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
    void shouldReturnEmptyResultsForNonNullLookupsWhenFormGroupDeliveryStatementTypeMapIsUnboundOrEmpty() {
        assertThat(unboundFormGroupDeliveryStatementTypeBiMap.deliveryStatementTypesFor(GROUP_1), empty());
        final var withEmpty = new FormGroupDeliveryStatementTypeBiMap(Map.of(GROUP_1, Set.of()));
        assertThat(withEmpty.deliveryStatementTypesFor(GROUP_1), empty());
        assertThat(withEmpty.formGroupsFor(DeliveryStatementType.values()[0]), empty());
    }

    @Test
    void shouldReturnEmptyResultsForNonNullLookupsWhenPresenterTypeDeliveryStatementTypeMapIsUnboundOrEmpty() {
        assertThat(unboundPresenterTypeDeliveryStatementTypeBiMap.deliveryStatementTypesFor(PRESENTER), empty());
        final var withEmpty = new PresenterTypeDeliveryStatementTypeBiMap(Map.of(PRESENTER, Set.of()));
        assertThat(withEmpty.deliveryStatementTypesFor(PRESENTER), empty());
        assertThat(withEmpty.presenterTypesFor(DeliveryStatementType.values()[0]), empty());
    }

    @Test
    void shouldReturnEmptyResultsForNonNullLookupsWhenPresenterTypeVerificationStatementTypeMapIsUnboundOrEmpty() {
        assertThat(unboundPresenterTypeVerificationStatementTypeBiMap.verificationStatementTypesFor(PRESENTER), empty());
        final var withEmpty = new PresenterTypeVerificationStatementTypeBiMap(Map.of(PRESENTER, Set.of()));
        assertThat(withEmpty.verificationStatementTypesFor(PRESENTER), empty());
        assertThat(withEmpty.presenterTypesFor(VerificationStatementType.values()[0]), empty());
    }

    @Test
    void shouldReturnEmptyResultsForNonNullLookupsWhenPresenterTypeAssociationStatementTypeMapIsUnboundOrEmpty() {
        assertThat(unboundPresenterTypeAssociationStatementTypeBiMap.associationStatementTypesFor(PRESENTER), empty());
        final var withEmpty = new PresenterTypeAssociationStatementTypeBiMap(Map.of(PRESENTER, Set.of()));
        assertThat(withEmpty.associationStatementTypesFor(PRESENTER), empty());
        assertThat(withEmpty.presenterTypesFor(AssociationStatementType.values()[0]), empty());
    }

    @Test
    void shouldReturnEmptyResultsForNonNullLookupsWhenPresenterTypeDataObjectTypeMapIsUnboundOrEmpty() {
        assertThat(unboundPresenterTypeDataObjectTypeBiMap.dataObjectTypesFor(PRESENTER), empty());
        final var withEmpty = new PresenterTypeDataObjectTypeBiMap(Map.of(PRESENTER, Set.of()));
        assertThat(withEmpty.dataObjectTypesFor(PRESENTER), empty());
        assertThat(withEmpty.presenterTypesFor(DataObjectType.values()[0]), empty());
    }
}

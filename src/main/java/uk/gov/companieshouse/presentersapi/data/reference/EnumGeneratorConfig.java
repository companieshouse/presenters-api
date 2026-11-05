package uk.gov.companieshouse.presentersapi.data.reference;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record EnumGeneratorConfig(
    @JsonProperty("output-package")
    OutputPackage outputPackage,
    Map<String, Source> sources
) {

    public enum Location {
        DICTIONARY_KEYS, DICTIONARY_VALUES
    }

    public enum UniquenessValidation {
        /**
        * <p>VALUES_PER_KEY_SET: Each dictionary value appears under AT MOST ONE dictionary key.</p>
         * <p>Validates that a value is unique across the entire set of keys in the file.
         * If a value appears under multiple keys, validation fails.</p>
         * <p>Use case: FormType in form-group-to-form.yaml</p>
         * <p>Each form type belongs to exactly one form group; if a form type appears
         * in multiple form groups, validation will report failure.</p>
         */
        VALUES_PER_KEY_SET,
        
        /**
        * <p>VALUES_PER_KEY: Each dictionary value can appear under MULTIPLE dictionary keys,</p>
         * <p>but must be unique within each key's value set (no duplicates per key).</p>
         * <p>Validates that within each key, all values are unique. Values can repeat
         * across different keys, but not within the same key.</p>
         * <p>Use case: PresenterType in form-group-to-presenter-types.yaml</p>
         * <p>Same presenter type can serve multiple form groups
         * (e.g., officer-employee works for firm-delivery AND individual-delivery).</p>
         */
        VALUES_PER_KEY
    }

    public record OutputPackage(
        @JsonProperty("enums")
        String enumPackage,
        @JsonProperty("bimaps")
        String bimapPackage
    ) {
    }

    public record Source(
        @JsonProperty("root-keys")
        List<String> rootKeys,
        @JsonProperty("key-enum")
        String keyEnum,
        @JsonProperty("value-enum")
        String valueEnum,
        Map<String, EnumDefinition> enums,
        @JsonProperty("require-non-empty-values-in")
        List<NonEmptyValuesRequirement> requireNonEmptyValuesIn
    ) {
        public Source {
            requireNonEmptyValuesIn = requireNonEmptyValuesIn == null
                ? List.of() : List.copyOf(requireNonEmptyValuesIn);
        }

        public Source(final List<String> rootKeys, final String keyEnum, final String valueEnum,
                      final Map<String, EnumDefinition> enums) {
            this(rootKeys, keyEnum, valueEnum, enums, List.of());
        }
    }

    public record NonEmptyValuesRequirement(
        @JsonProperty("target")
        String target,
        @JsonProperty("excluded-keys")
        List<String> excludedKeys
    ) {
        public NonEmptyValuesRequirement {
            excludedKeys = excludedKeys == null ? List.of() : List.copyOf(excludedKeys);
        }
    }

    public record EnumDefinition(
        Location location,
        @JsonProperty("dictionary-keys-reference")
        String dictionaryKeysReference,
        EnumValidation validation
    ) {
    }

    public record EnumValidation(
        @JsonProperty("uniqueness")
        UniquenessValidation uniquenessValidation,
        @JsonProperty("validate-as-keys-in-other-sources")
        Boolean validateAsKeysInOtherSources
    ) {
    }
}

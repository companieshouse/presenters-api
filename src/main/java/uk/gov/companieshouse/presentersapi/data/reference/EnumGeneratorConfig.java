package uk.gov.companieshouse.presentersapi.data.reference;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        * VALUES_PER_KEY_SET: Each dictionary value appears under AT MOST ONE dictionary key.
         * 
         * Validates that a value is unique across the entire set of keys in the file.
         * If a value appears under multiple keys, validation fails.
         * 
         * Use case: FormType in form-type-by-form-group.yaml
         * Each form type belongs to exactly one form group; if a form type appears
         * in multiple form groups, validation will report failure.
         */
        VALUES_PER_KEY_SET,
        
        /**
        * VALUES_PER_KEY: Each dictionary value can appear under MULTIPLE dictionary keys,
         * but must be unique within each key's value set (no duplicates per key).
         * 
         * Validates that within each key, all values are unique. Values can repeat
         * across different keys, but not within the same key.
         * 
         * Use case: PresenterType in presenter-type-by-form-group.yaml
         * Same presenter type can serve multiple form groups
         * (e.g., officer-employee works for firm-delivery AND individual-delivery).
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
        java.util.List<String> rootKeys,
        @JsonProperty("key-enum")
        String keyEnum,
        @JsonProperty("value-enum")
        String valueEnum,
        Map<String, EnumDefinition> enums
    ) {
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


package uk.gov.companieshouse.presentersapi.data.reference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

public class BiMapSourceCodeBuilder {

    private BiMapSourceCodeBuilder() {
        // Utility class
    }

    /**
     * Builds complete BiMap source code for a given key-value enum pair.
     *
     * <p>The generated BiMap structure is determined by the uniqueness validation constraint:
     * <ul>
     *   <li><b>VALUES_PER_KEY_SET:</b> Generates reverse mapping as singular
     *       (EnumMap&lt;ValueEnum, KeyEnum&gt;), with validation that each value appears
     *       under at most one key. Uses putIfAbsent() to enforce uniqueness.</li>
     *   <li><b>VALUES_PER_KEY:</b> Generates reverse mapping as plural
     *       (EnumMap&lt;ValueEnum, EnumSet&lt;KeyEnum&gt;&gt;), allowing values to appear
     *       under multiple keys. Uses computeIfAbsent() for efficient set population.</li>
     * </ul>
     *
     * <p>Forward mapping is always ONE-TO-MANY (EnumMap&lt;KeyEnum, EnumSet&lt;ValueEnum&gt;&gt;)
     * regardless of uniqueness validation mode, because YAML dictionary structure guarantees keys
     * map to value arrays.
     *
     * @param biMapPkg package for the generated BiMap class
     * @param className name of the BiMap class to generate
     * @param keyEnumName name of the key enum type
     * @param valueEnumName name of the value enum type
     * @param configKeyName YAML prefix for @ConfigurationProperties
     * @param yamlPropertyName property name from YAML (e.g., "by-form-group")
     * @param uniquenessValidation uniqueness validation mode (VALUES_PER_KEY or VALUES_PER_KEY_SET)
     * @return complete Java source code for the BiMap class
     */
    public static String buildBiMapSource(
            final String biMapPkg, final String className,
            final String keyEnumName, final String valueEnumName,
            final String configKeyName, final String yamlPropertyName,
            final EnumGeneratorConfig.UniquenessValidation uniquenessValidation) {
        final var enumPkg = enumPackageFor(biMapPkg);
        if (uniquenessValidation == EnumGeneratorConfig.UniquenessValidation.VALUES_PER_KEY_SET) {
            return buildUniquePerKeySetBiMap(biMapPkg, enumPkg, className, keyEnumName, valueEnumName, configKeyName, yamlPropertyName);
        } else {
            return buildUniqueValuesPerKeySetBiMap(biMapPkg, enumPkg, className, keyEnumName, valueEnumName, configKeyName, yamlPropertyName);
        }
    }

    private static String enumPackageFor(final String biMapPkg) {
        return biMapPkg.replaceAll("\\.bimaps?$", ".enums");
    }

    /**
     * Builds VALUES_PER_KEY_SET BiMap source code.
     *
     * <p>Reverse mapping type: EnumMap&lt;ValueEnum, KeyEnum&gt; (singular, not a set)
     * Reverse method: keyFor(value) returns KeyEnum
     * Validation: Throws IllegalStateException if any value appears under multiple keys
     *
     * <p>Implementation: Uses putIfAbsent() with duplicate detection. During construction,
     * if a value is already mapped to a different key, an exception is thrown immediately.
     */
    @SuppressWarnings("java:S1192") // Suppress "String literals should not be duplicated" warning
    private static String buildUniquePerKeySetBiMap(
            final String biMapPkg, final String enumPkg, final String className,
            final String keyEnumName, final String valueEnumName,
            final String configKeyName, final String yamlPropertyName) {
       final var propertyParamName = CaseUtils.toCamelCase(yamlPropertyName, false, '-');
       final var valueParamName = StringUtils.uncapitalize(valueEnumName);
        
        return "package " + biMapPkg + ";\n\n"
            + "import java.util.EnumMap;\n"
            + "import java.util.EnumSet;\n"
            + "import java.util.Set;\n"
            + "import org.springframework.boot.context.properties.ConfigurationProperties;\n"
            + "import java.util.Map;\n"
            + "import " + enumPkg + "." + keyEnumName + ";\n"
            + "import " + enumPkg + "." + valueEnumName + ";\n\n"
            + "@ConfigurationProperties(prefix = \"" + configKeyName + "\")\n"
            + "public class " + className + " {\n"
            + "    private final EnumMap<" + keyEnumName + ", EnumSet<" + valueEnumName + ">> by" + keyEnumName + ";\n"
            + "    private final EnumMap<" + valueEnumName + ", " + keyEnumName + "> by" + valueEnumName + ";\n\n"
            + "    public " + className + "(final Map<" + keyEnumName + ", Set<" + valueEnumName + ">> " + propertyParamName + ") {\n"
            + "        final var forwardMapping = new EnumMap<" + keyEnumName + ", EnumSet<" + valueEnumName + ">>(" + keyEnumName + ".class);\n"
            + "        final var reverseMapping = new EnumMap<" + valueEnumName + ", " + keyEnumName + ">(" + valueEnumName + ".class);\n\n"
            + "        if (" + propertyParamName + " != null) {\n"
            + "            " + propertyParamName + ".forEach((key, values) -> {\n"
            + "                final var enumValues = values.isEmpty()\n"
            + "                    ? EnumSet.noneOf(" + valueEnumName + ".class)\n"
            + "                    : EnumSet.copyOf(values);\n"
            + "                forwardMapping.put(key, enumValues);\n\n"
            + "                for (final var value : enumValues) {\n"
            + "                    final var existing = reverseMapping.putIfAbsent(value, key);\n"
            + "                    if (existing != null && existing != key) {\n"
            + "                        throw new IllegalStateException(\n"
            + "                            \"" + valueEnumName + " '\" + value + \"' mapped by both '\" + existing + \"' and '\" + key + \"'\"\n"
            + "                        );\n"
            + "                    }\n"
            + "                }\n"
            + "            });\n"
            + "        }\n\n"
            + "        this.by" + keyEnumName + " = forwardMapping;\n"
            + "        this.by" + valueEnumName + " = reverseMapping;\n"
            + "    }\n\n"
            + "    public EnumSet<" + valueEnumName + "> " + valueParamName + "sFor(final " + keyEnumName + " key) {\n"
            + "        return by" + keyEnumName + ".getOrDefault(key, EnumSet.noneOf(" + valueEnumName + ".class));\n"
            + "    }\n\n"
           + "    public " + keyEnumName + " " + StringUtils.uncapitalize(keyEnumName) + "For(final " + valueEnumName + " value) {\n"
            + "        return by" + valueEnumName + ".get(value);\n"
            + "    }\n"
            + "}\n";
    }

    /**
     * Builds VALUES_PER_KEY BiMap source code.
     *
     * <p>Reverse mapping type: EnumMap&lt;ValueEnum, EnumSet&lt;KeyEnum&gt;&gt; (plural, a set)
     * Reverse method: keysFor(value) returns EnumSet&lt;KeyEnum&gt;
     * Validation: Allows same value to appear under multiple keys, but not duplicate values
     *            under the same key (enforced by YAML structure and configuration validation)
     *
     * <p>Implementation: Uses computeIfAbsent() to efficiently populate reverse mappings.
     * Each value can accumulate multiple keys in its reverse set.
     */
    private static String buildUniqueValuesPerKeySetBiMap(
            final String biMapPkg, final String enumPkg, final String className,
            final String keyEnumName, final String valueEnumName,
            final String configKeyName, final String yamlPropertyName) {
       final var propertyParamName = CaseUtils.toCamelCase(yamlPropertyName, false, '-');
       final var valueParamName = StringUtils.uncapitalize(valueEnumName);
        
        return "package " + biMapPkg + ";\n\n"
            + "import java.util.EnumMap;\n"
            + "import java.util.EnumSet;\n"
            + "import java.util.Set;\n"
            + "import org.springframework.boot.context.properties.ConfigurationProperties;\n"
            + "import java.util.Map;\n"
            + "import " + enumPkg + "." + keyEnumName + ";\n"
            + "import " + enumPkg + "." + valueEnumName + ";\n\n"
            + "@ConfigurationProperties(prefix = \"" + configKeyName + "\")\n"
            + "public class " + className + " {\n"
            + "    private final EnumMap<" + keyEnumName + ", EnumSet<" + valueEnumName + ">> by" + keyEnumName + ";\n"
            + "    private final EnumMap<" + valueEnumName + ", EnumSet<" + keyEnumName + ">> by" + valueEnumName + ";\n\n"
            + "    public " + className + "(final Map<" + keyEnumName + ", Set<" + valueEnumName + ">> " + propertyParamName + ") {\n"
            + "        final var forwardMapping = new EnumMap<" + keyEnumName + ", EnumSet<" + valueEnumName + ">>(" + keyEnumName + ".class);\n"
            + "        final var reverseMapping = new EnumMap<" + valueEnumName + ", EnumSet<" + keyEnumName + ">>(" + valueEnumName + ".class);\n\n"
            + "        if (" + propertyParamName + " != null) {\n"
            + "            " + propertyParamName + ".forEach((key, values) -> {\n"
            + "                final var enumValues = values.isEmpty()\n"
            + "                    ? EnumSet.noneOf(" + valueEnumName + ".class)\n"
            + "                    : EnumSet.copyOf(values);\n"
            + "                forwardMapping.put(key, enumValues);\n\n"
            + "                for (final var value : enumValues) {\n"
            + "                    reverseMapping.computeIfAbsent(value, k -> EnumSet.noneOf(" + keyEnumName + ".class)).add(key);\n"
            + "                }\n"
            + "            });\n"
            + "        }\n\n"
            + "        this.by" + keyEnumName + " = forwardMapping;\n"
            + "        this.by" + valueEnumName + " = reverseMapping;\n"
            + "    }\n\n"
            + "    public EnumSet<" + valueEnumName + "> " + valueParamName + "sFor(final " + keyEnumName + " key) {\n"
            + "        return by" + keyEnumName + ".getOrDefault(key, EnumSet.noneOf(" + valueEnumName + ".class));\n"
            + "    }\n\n"
           + "    public EnumSet<" + keyEnumName + "> " + StringUtils.uncapitalize(keyEnumName) + "sFor(final " + valueEnumName + " value) {\n"
            + "        return by" + valueEnumName + ".getOrDefault(value, EnumSet.noneOf(" + keyEnumName + ".class));\n"
            + "    }\n"
            + "}\n";
    }
}

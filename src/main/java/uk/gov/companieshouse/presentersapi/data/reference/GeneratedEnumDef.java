package uk.gov.companieshouse.presentersapi.data.reference;

import java.util.Set;

/**
 * Represents a generated enum definition with its extracted values and target package.
 *
 * @param name the enum class name
 * @param values the set of values extracted for this enum from YAML sources
 * @param outputPackage the target package for the generated enum class
 */
public record GeneratedEnumDef(String name, Set<String> values, String outputPackage) {}


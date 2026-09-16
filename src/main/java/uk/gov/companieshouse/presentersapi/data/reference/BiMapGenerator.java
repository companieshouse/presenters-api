package uk.gov.companieshouse.presentersapi.data.reference;

import static uk.gov.companieshouse.presentersapi.PresentersApiApplication.APP_NAMESPACE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

/**
 * Handles BiMap generation from enum pair mappings.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate BiMap class names from enum pairs
 *   <li>Determine uniqueness validation mode for reverse mappings
 *   <li>Generate and write BiMap source files
 * </ul>
 */
class BiMapGenerator {
    private static final Logger logger = LoggerFactory.getLogger(APP_NAMESPACE);

    private final EnumGeneratorConfig config;
    private final Path outputDir;

    BiMapGenerator(final EnumGeneratorConfig config, final Path outputDir) {
        this.config = config;
        this.outputDir = outputDir;
    }

    // (Part of 6-phase pipeline in EnumBiMapGenerator.generate())
    // Phase 6: Generate BiMap classes for enum pair mappings to enable bidirectional lookups.
    void generateBiMapsFromSources() throws IOException {
        try {
            final var biMapPkg = config.outputPackage().bimapPackage();
            
            config.sources().forEach((sourceFile, sourceConfig) -> {
                try {
                    // Only generate BiMap if both key-enum and value-enum are defined
                    if (sourceConfig.keyEnum() == null || sourceConfig.valueEnum() == null) {
                        return;
                    }

                    final var keyEnumName = sourceConfig.keyEnum();
                    final var valueEnumName = sourceConfig.valueEnum();

                    // Determine uniqueness validation mode for the reverse mapping
                    final var uniquenessValidation = sourceConfig.enums().values().stream()
                        .filter(enumDef -> enumDef.location() == EnumGeneratorConfig.Location.DICTIONARY_VALUES)
                        .findFirst()
                        .map(enumDef -> enumDef.validation() != null ? enumDef.validation().uniquenessValidation() : null)
                        .orElse(null);

                    // Generate and write BiMap source to data.bimap package
                    final var biMapClassName = generateBiMapClassName(keyEnumName, valueEnumName);
                    final var configKeyName = sourceConfig.rootKeys().get(0);
                    final var yamlPropertyName = sourceConfig.rootKeys().get(1);
                    final var biMapSource = BiMapSourceCodeBuilder.buildBiMapSource(
                        biMapPkg,
                        biMapClassName,
                        keyEnumName,
                        valueEnumName,
                        configKeyName,
                        yamlPropertyName,
                        uniquenessValidation
                    );
                    writeBiMapFile(outputDir, biMapPkg, biMapClassName, biMapSource);
                    logger.info("  [Generated] BiMap '" + biMapClassName + "' for " + keyEnumName + " ↔ " + valueEnumName);
                } catch (final IOException e) {
                    throw new GeneratorValidator.WrappedIOException(e);
                }
            });
        } catch (final GeneratorValidator.WrappedIOException e) {
            throw e.unwrap();
        }
    }

    // Generate a BiMap class name from two enum names using camelCase convention.
    private String generateBiMapClassName(final String keyEnumName, final String valueEnumName) {
        return keyEnumName + valueEnumName + "BiMap";
    }

    // Write BiMap source file to output directory.
    private void writeBiMapFile(
            final Path outputDir, final String pkg, final String className, final String source)
            throws IOException {
        final var packagePath = outputDir.resolve(pkg.replace('.', '/'));
        Files.createDirectories(packagePath);
        Files.writeString(packagePath.resolve(className + ".java"), source);
    }
}

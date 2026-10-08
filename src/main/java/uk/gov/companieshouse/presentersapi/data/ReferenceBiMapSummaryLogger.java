package uk.gov.companieshouse.presentersapi.data;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.presentersapi.PresentersApiApplication;

/**
 * Logs a summary line for each BiMap bean in the application context once the application is ready.
 *
 * <p>A BiMap with no forward entries is logged at error level: it normally means its reference data
 * file was not imported via spring.config.import, so every lookup would silently return nothing.
 */
@Component
public class ReferenceBiMapSummaryLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(PresentersApiApplication.APP_NAMESPACE);

    private final List<ReferenceBiMap> biMaps;
    private final Consumer<String> infoSink;
    private final Consumer<String> errorSink;

    @Autowired
    public ReferenceBiMapSummaryLogger(final List<ReferenceBiMap> biMaps) {
        this(biMaps, LOGGER::info, LOGGER::error);
    }

    ReferenceBiMapSummaryLogger(
            final List<ReferenceBiMap> biMaps, final Consumer<String> infoSink, final Consumer<String> errorSink) {
        this.biMaps = biMaps;
        this.infoSink = infoSink;
        this.errorSink = errorSink;
    }

    @EventListener(ApplicationReadyEvent.class)
    void logSummaries() {
        biMaps.stream()
            .sorted(Comparator.comparing(ReferenceBiMapSummaryLogger::nameOf))
            .forEach(biMap -> {
                if (biMap.forwardEntryCount() == 0) {
                    errorSink.accept(summaryLine(biMap)
                        + " - reference data may not have been imported (check spring.config.import)");
                } else {
                    infoSink.accept(summaryLine(biMap));
                }
            });
    }

    public List<String> summaryLines() {
        return biMaps.stream()
            .sorted(Comparator.comparing(ReferenceBiMapSummaryLogger::nameOf))
            .map(ReferenceBiMapSummaryLogger::summaryLine)
            .toList();
    }

    private static String nameOf(final ReferenceBiMap biMap) {
        return biMap.getClass().getSimpleName();
    }

    private static String summaryLine(final ReferenceBiMap biMap) {
        return "BiMap '" + nameOf(biMap) + "' created: "
            + biMap.forwardEntryCount() + " forward mappings (keys), "
            + biMap.reverseEntryCount() + " reverse mappings (values)";
    }
}

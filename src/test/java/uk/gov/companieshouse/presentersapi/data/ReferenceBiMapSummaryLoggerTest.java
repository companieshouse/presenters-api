package uk.gov.companieshouse.presentersapi.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReferenceBiMapSummaryLoggerTest {

    private static class AlphaBiMap implements ReferenceBiMap {
        @Override
        public int forwardEntryCount() {
            return 3;
        }

        @Override
        public int reverseEntryCount() {
            return 5;
        }
    }

    private static class BetaBiMap implements ReferenceBiMap {
        @Override
        public int forwardEntryCount() {
            return 0;
        }

        @Override
        public int reverseEntryCount() {
            return 0;
        }
    }

    @Test
    void shouldReportNameAndEntryCountsInEachDirectionWhenSummarising() {
        final var logger = new ReferenceBiMapSummaryLogger(List.of(new AlphaBiMap()));

        assertThat(logger.summaryLines(),
            contains("BiMap 'AlphaBiMap' created: 3 forward mappings (keys), 5 reverse mappings (values)"));
    }

    @Test
    void shouldSortLinesByNameAndIncludeEmptyBiMapsWhenSeveralBiMapsExist() {
        final var logger = new ReferenceBiMapSummaryLogger(List.of(new BetaBiMap(), new AlphaBiMap()));

        assertThat(logger.summaryLines(), contains(
            "BiMap 'AlphaBiMap' created: 3 forward mappings (keys), 5 reverse mappings (values)",
            "BiMap 'BetaBiMap' created: 0 forward mappings (keys), 0 reverse mappings (values)"));
    }

    @Test
    void shouldProduceNoLinesWhenThereAreNoBiMaps() {
        assertThat(new ReferenceBiMapSummaryLogger(List.of()).summaryLines(), empty());
    }

    @Test
    void shouldLogAtInfoLevelWhenBiMapHasForwardEntries() {
        final var info = new ArrayList<String>();
        final var error = new ArrayList<String>();

        new ReferenceBiMapSummaryLogger(List.of(new AlphaBiMap()), info::add, error::add).logSummaries();

        assertThat(info, contains("BiMap 'AlphaBiMap' created: 3 forward mappings (keys), 5 reverse mappings (values)"));
        assertThat(error, empty());
    }

    @Test
    void shouldLogAtErrorLevelWhenBiMapHasNoForwardEntries() {
        final var info = new ArrayList<String>();
        final var error = new ArrayList<String>();

        new ReferenceBiMapSummaryLogger(List.of(new BetaBiMap()), info::add, error::add).logSummaries();

        assertThat(info, empty());
        assertThat(error, contains("BiMap 'BetaBiMap' created: 0 forward mappings (keys), 0 reverse mappings (values)"
            + " - reference data may not have been imported (check spring.config.import)"));
    }

    @Test
    void shouldSplitLinesByLevelWhenBiMapsAreMixed() {
        final var info = new ArrayList<String>();
        final var error = new ArrayList<String>();

        new ReferenceBiMapSummaryLogger(List.of(new BetaBiMap(), new AlphaBiMap()), info::add, error::add).logSummaries();

        assertThat(info.size(), is(1));
        assertThat(error.size(), is(1));
    }

    @Test
    void shouldLogAtInfoLevelWhenBiMapHasNoReverseEntries() {
        final var info = new ArrayList<String>();
        final var error = new ArrayList<String>();
        final ReferenceBiMap keysOnly = new ReferenceBiMap() {
            @Override
            public int forwardEntryCount() {
                return 2;
            }

            @Override
            public int reverseEntryCount() {
                return 0;
            }
        };

        new ReferenceBiMapSummaryLogger(List.of(keysOnly), info::add, error::add).logSummaries();

        assertThat(info.size(), is(1));
        assertThat(error, empty());
    }

    @Test
    void shouldLogWithoutFailureWhenUsingPublicConstructor() {
        final var logger = new ReferenceBiMapSummaryLogger(List.of(new AlphaBiMap(), new BetaBiMap()));

        assertDoesNotThrow(logger::logSummaries, "Logging to the real logger should not fail for populated or empty BiMaps");
    }
}

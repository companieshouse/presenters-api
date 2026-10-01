package uk.gov.companieshouse.presentersapi.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.logging.Logger;

class LoggingConfigTest {

    private LoggingConfig loggingConfig;

    @BeforeEach
    void setUp() {
        loggingConfig = new LoggingConfig();
    }

    @Test
    void createsLogger() {
        assertThat(loggingConfig.logger("presenters-api"), isA(Logger.class));
    }
}

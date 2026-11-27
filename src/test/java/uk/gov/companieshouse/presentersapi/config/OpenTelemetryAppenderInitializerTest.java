package uk.gov.companieshouse.presentersapi.config;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

class OpenTelemetryAppenderInitializerTest {

    @Test
    void afterPropertiesSetDelegatesToInstallAppender() {
        final OpenTelemetry openTelemetry = OpenTelemetry.noop();
        final OpenTelemetryAppenderInitializer initializer =
                spy(new OpenTelemetryAppenderInitializer(openTelemetry));

        doNothing().when(initializer).installAppender(openTelemetry);

        initializer.afterPropertiesSet();

        verify(initializer).installAppender(openTelemetry);
    }
}

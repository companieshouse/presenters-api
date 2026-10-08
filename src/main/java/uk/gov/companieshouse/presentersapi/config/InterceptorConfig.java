package uk.gov.companieshouse.presentersapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.interceptor.RequestLoggingInterceptor;

/**
 * Configuration class for setting up interceptors in the application.
 */
@Configuration
@ComponentScan("uk.gov.companieshouse.presentersapi")
public class InterceptorConfig implements WebMvcConfigurer {
    @SuppressWarnings("java:S1075") // Suppress SonarQube warning for hardcoded paths
    private static final int ORDERED_LAST = Integer.MAX_VALUE;

    private final Logger logger;

    public InterceptorConfig(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        addLoggingInterceptor(registry);
    }

    private void addLoggingInterceptor(final InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor())
                .order(ORDERED_LAST);
    }

    @Bean("chsLoggingInterceptor")
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor(logger);
    }
}

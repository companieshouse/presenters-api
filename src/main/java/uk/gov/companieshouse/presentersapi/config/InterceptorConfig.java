package uk.gov.companieshouse.presentersapi.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.interceptor.RequestLoggingInterceptor;

/**
 * Configuration class for setting up interceptors in the application.
 */
@Configuration
@ComponentScan("uk.gov.companieshouse.presentersapi")
public class InterceptorConfig implements WebMvcConfigurer {
    @SuppressWarnings("java:S1075") // Suppress SonarQube warning for hardcoded paths
    private static final String BASE_PATH = "/presenters";
    private static final String VALIDATION_PATH = BASE_PATH + "/validation";

    private final Logger logger;

    public InterceptorConfig(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void addInterceptors(final @NonNull InterceptorRegistry registry) {
        addLoggingInterceptor(registry);
    }

    private void addLoggingInterceptor(final @NonNull InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor())
                .order(0);
    }

    @Bean("chsLoggingInterceptor")
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor(logger);
    }

    private void addInternalUserInterceptor(final @NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new InternalUserInterceptor())
                .addPathPatterns(VALIDATION_PATH)
                .order(1);
    }


}

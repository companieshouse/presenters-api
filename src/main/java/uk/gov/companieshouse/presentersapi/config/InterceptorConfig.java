package uk.gov.companieshouse.presentersapi.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.interceptor.MappablePermissionsInterceptor;
import uk.gov.companieshouse.api.interceptor.PermissionsMapping;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.api.util.security.Permission;
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
    private static final String PRESENTER_TYPE_PATH = "%s/presenter-type".formatted(BASE_PATH);

    private final Logger logger;

    public InterceptorConfig(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void addInterceptors(final @NonNull InterceptorRegistry registry) {
        addLoggingInterceptor(registry);
        addTokenPermissionsInterceptor(registry);
        addRequestPermissionsInterceptor(registry);
    }

    private void addLoggingInterceptor(final @NonNull InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor())
            .addPathPatterns(BASE_PATH + "/**")
                .order(0);
    }

    private void addTokenPermissionsInterceptor(final @NonNull InterceptorRegistry registry) {
        registry.addInterceptor(tokenPermissionsInterceptor())
                .addPathPatterns(PRESENTER_TYPE_PATH)
                .order(2);
    }

    private void addRequestPermissionsInterceptor(final @NonNull InterceptorRegistry registry) {
        // Both user_profile and acsp_associations permissions are required
        registry
            .addInterceptor(userProfileInterceptor(httpGetWithReadPermissionsMapping()))
            .addPathPatterns(PRESENTER_TYPE_PATH)
            .order(3);
        registry
            .addInterceptor(acspAssociationsInterceptor(httpGetWithReadPermissionsMapping()))
            .addPathPatterns(PRESENTER_TYPE_PATH)
            .order(4);
    }

    @Bean("chsLoggingInterceptor")
    public @NonNull RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor(logger);
    }

    @Bean("chsTokenPermissionsCaptor")
    public TokenPermissionsInterceptor tokenPermissionsInterceptor() {
        return new TokenPermissionsInterceptor();
    }

    @Bean("chsUserProfileInterceptor")
    public MappablePermissionsInterceptor userProfileInterceptor(final @NonNull PermissionsMapping permissionsMapping) {
        return new MappablePermissionsInterceptor(Permission.Key.USER_PROFILE, permissionsMapping);
    }

    @Bean("chsAcspAssociationsInterceptor")
    public MappablePermissionsInterceptor acspAssociationsInterceptor(final @NonNull PermissionsMapping permissionsMapping) {
        return new MappablePermissionsInterceptor(Permission.Key.ACSP_ASSOCIATIONS, permissionsMapping);
    }

    @Bean("chsHttpGetWithReadPermissionsMapping")
    public @NonNull PermissionsMapping httpGetWithReadPermissionsMapping() {
        return PermissionsMapping
                   .builder()
                   .defaultRequireNone()
                   .mappedRequireAnyOf("GET", Permission.Value.READ)
                   .build();
    }
}

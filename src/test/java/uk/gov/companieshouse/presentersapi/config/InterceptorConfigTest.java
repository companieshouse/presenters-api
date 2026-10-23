package uk.gov.companieshouse.presentersapi.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.api.interceptor.MappablePermissionsInterceptor;
import uk.gov.companieshouse.api.interceptor.PermissionsMapping;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.api.util.security.Permission;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.interceptor.RequestLoggingInterceptor;

@ExtendWith(MockitoExtension.class)
class InterceptorConfigTest {

    @Mock
    private Logger logger;

    @Test
    void shouldRegisterInterceptorsForTheirConfiguredPathsAndOrder() {
        final var registry = new TestInterceptorRegistry();
        final var interceptorConfig = new InterceptorConfig(logger);

        interceptorConfig.addInterceptors(registry);

        final var registeredInterceptors = registry.registeredInterceptors();

        assertThat(registeredInterceptors, hasSize(5));
        assertRegistration(registeredInterceptors.get(0), RequestLoggingInterceptor.class, "/presenters/**");
        assertRegistration(
            registeredInterceptors.get(1),
            InternalUserInterceptor.class,
            "/presenters/validation"
        );
        assertRegistration(
            registeredInterceptors.get(2),
            TokenPermissionsInterceptor.class,
            "/presenters/presenter-type"
        );
        assertRegistration(
            registeredInterceptors.get(3),
            MappablePermissionsInterceptor.class,
            "/presenters/presenter-type"
        );
        assertPermissionConfiguration(
            registeredInterceptors.get(3),
            Permission.Key.USER_PROFILE
        );
        assertRegistration(
            registeredInterceptors.get(4),
            MappablePermissionsInterceptor.class,
            "/presenters/presenter-type"
        );
        assertPermissionConfiguration(
            registeredInterceptors.get(4),
            Permission.Key.ACSP_ASSOCIATIONS
        );
    }

    @Test
    void shouldCreateRequestLoggingInterceptorWhenBeanRequested() {
        final var interceptorConfig = new InterceptorConfig(logger);
        final var interceptor = interceptorConfig.requestLoggingInterceptor();

        assertThat(interceptor, is(isA(RequestLoggingInterceptor.class)));
    }

    @Test
    void shouldCreateInternalUserInterceptorWhenBeanRequested() {
        final var interceptorConfig = new InterceptorConfig(logger);
        final var interceptor = interceptorConfig.internalAppUserInterceptor();

        assertThat(interceptor, is(isA(InternalUserInterceptor.class)));
    }

    @Test
    void shouldCreateTokenPermissionsInterceptorWhenBeanRequested() {
        final var interceptorConfig = new InterceptorConfig(logger);
        final var interceptor = interceptorConfig.tokenPermissionsInterceptor();

        assertThat(interceptor, is(isA(TokenPermissionsInterceptor.class)));
    }

    @Test
    void shouldCreateUserProfileInterceptorWhenBeanRequested() {
        final var interceptorConfig = new InterceptorConfig(logger);
        final var permissionsMapping = interceptorConfig.httpGetWithReadPermissionsMapping();

        final var interceptor = interceptorConfig.userProfileInterceptor(permissionsMapping);

        assertThat(interceptor, is(isA(MappablePermissionsInterceptor.class)));
    }

    @Test
    void shouldCreateAcspAssociationsInterceptorWhenBeanRequested() {
        final var interceptorConfig = new InterceptorConfig(logger);
        final var permissionsMapping = interceptorConfig.httpGetWithReadPermissionsMapping();

        final var interceptor = interceptorConfig.acspAssociationsInterceptor(permissionsMapping);

        assertThat(interceptor, is(isA(MappablePermissionsInterceptor.class)));
    }

    @Test
    void shouldRequireReadPermissionForGetAndNoPermissionForOtherMethods() {
        final var interceptorConfig = new InterceptorConfig(logger);
        final var permissionsMapping = interceptorConfig.httpGetWithReadPermissionsMapping();

        final var getPermissions = permissionsMapping.apply("GET");
        final var postPermissions = permissionsMapping.apply("POST");
         assertThat(getPermissions, contains(Permission.Value.READ));
        assertThat(postPermissions, is(empty()));
    }

    private static void assertRegistration(
        final MappedInterceptor registration,
        final Class<? extends HandlerInterceptor> interceptorType,
        final String pathPattern
    ) {
        assertThat(registration.getInterceptor(), isA(interceptorType));
        final var includePathPatterns = registration.getIncludePathPatterns();
        assertNotNull(includePathPatterns);
        assertThat(Arrays.asList(includePathPatterns), contains(pathPattern));
    }

    private static void assertPermissionConfiguration(
        final MappedInterceptor registration,
        final Permission.Key expectedPermissionKey
    ) {
        final var interceptor = registration.getInterceptor();
        assertThat(ReflectionTestUtils.getField(interceptor, "permissionKey"), is(expectedPermissionKey));

        final var permissionsMapping = assertInstanceOf(
            PermissionsMapping.class,
            ReflectionTestUtils.getField(interceptor, "permissionsMapping")
        );
        assert permissionsMapping != null;
        final var getPermissions = permissionsMapping.apply("GET");
        final var postPermissions = permissionsMapping.apply("POST");
        assertThat(getPermissions, contains(Permission.Value.READ));
        assertThat(postPermissions, is(empty()));
    }

    private static final class TestInterceptorRegistry extends InterceptorRegistry {
        private List<MappedInterceptor> registeredInterceptors() {
            return getInterceptors().stream()
                .map(MappedInterceptor.class::cast)
                .toList();
        }
    }
}

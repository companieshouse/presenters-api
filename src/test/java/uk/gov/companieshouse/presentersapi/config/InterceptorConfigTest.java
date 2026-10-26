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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest
    @MethodSource("interceptorRegistrations")
    void shouldRegisterInterceptorsForTheirConfiguredPathsAndOrder(
        final int registrationIndex,
        final Class<? extends HandlerInterceptor> interceptorType,
        final List<String> pathPatterns
    ) {
        final var registry = new TestInterceptorRegistry();
        final var interceptorConfig = new InterceptorConfig(logger);

        interceptorConfig.addInterceptors(registry);

        final var registeredInterceptors = registry.registeredInterceptors();

        assertThat(registeredInterceptors, hasSize(5));
        final var registration = registeredInterceptors.get(registrationIndex);
        assertThat(registration.getInterceptor(), isA(interceptorType));
        final var includePathPatterns = registration.getIncludePathPatterns();
        assertNotNull(includePathPatterns);
        assertThat(Arrays.asList(includePathPatterns), is(pathPatterns));
    }

    private static Stream<Arguments> interceptorRegistrations() {
        final var permissionPaths = List.of("/presenters/presenter-type", "/presenters/presenter-details");
        return Stream.of(
            Arguments.of(0, RequestLoggingInterceptor.class, List.of("/presenters/**")),
            Arguments.of(1, InternalUserInterceptor.class, List.of("/presenters/validation")),
            Arguments.of(2, TokenPermissionsInterceptor.class, permissionPaths),
            Arguments.of(3, MappablePermissionsInterceptor.class, permissionPaths),
            Arguments.of(4, MappablePermissionsInterceptor.class, permissionPaths)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "3, USER_PROFILE",
        "4, ACSP_ASSOCIATIONS"
    })
    void shouldRequireConfiguredPermissionWhenPermissionsInterceptorRegistered(
        final int registrationIndex,
        final Permission.Key expectedPermissionKey
    ) {
        final var registry = new TestInterceptorRegistry();
        final var interceptorConfig = new InterceptorConfig(logger);

        interceptorConfig.addInterceptors(registry);

        assertPermissionConfiguration(registry.registeredInterceptors().get(registrationIndex), expectedPermissionKey);
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

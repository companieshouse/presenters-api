package uk.gov.companieshouse.presentersapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;
import uk.gov.companieshouse.presentersapi.repository.ExemptUserRepository;

import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ExemptUserServiceImplTest {

    @Mock
    private ExemptUserRepository repository;
    private ExemptUserServiceImpl service;
    @Mock
    private Logger logger;

    @BeforeEach
    void setUp(){
        this.service = new ExemptUserServiceImpl(repository, logger);
    }

    @Test
    void returnsExemptUserByEmail() {
        ExemptUserDao mockUser = new ExemptUserDao();
        mockUser.setEmail("test@example.com");
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        Optional<ExemptUserDao> result = service.getExemptUserByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        verify(repository).findByEmail("test@example.com");
    }

    @Test
    void returnsExemptUserById() {
        ExemptUserDao mockUser = new ExemptUserDao();
        mockUser.setId("6ba129e2939hf");

        when(repository.findById("id")).thenReturn(Optional.of(mockUser));
        Optional<ExemptUserDao> result = service.getExemptUserById("id");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("6ba129e2939hf");
        verify(repository).findById("id");
    }

    @Test
    void returnsNoSuchExemptUserWhenGettingByEmail() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        Optional<ExemptUserDao> result = service.getExemptUserByEmail("missing@example.com");

        assertThat(result).isEmpty();
        verify(repository).findByEmail("missing@example.com");
        verify(logger).infoContext(
                "missing@example.com",
                "No Such exempt user with email: ",
                Map.of("email", "missing@example.com"));
    }

    @Test
    void returnsNoSuchExemptUserWhenGettingById() {
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        Optional<ExemptUserDao> result = service.getExemptUserById("missing-id");

        assertThat(result).isEmpty();
        verify(repository).findById("missing-id");
        verify(logger).infoContext(
                "missing-id",
                "No Such exempt user with id: ",
                Map.of("id", "missing-id"));
    }

    @Test
    void returnsTrueWhenExemptUserByEmail() {
        when(repository.existsByEmail("test@example.com")).thenReturn(true);
        boolean result = service.isExemptUserByEmail("test@example.com");

        assertThat(result).isTrue();
        verify(repository).existsByEmail("test@example.com");
    }

    @Test
    void returnsFalseWhenNotExemptUserByEmail(){
        when(repository.existsByEmail("test@example.com")).thenReturn(false);
        boolean result = service.isExemptUserByEmail("test@example.com");

        assertThat(result).isFalse();
        verify(repository).existsByEmail("test@example.com");
    }

    @Test
    void returnsTrueWhenExemptUserById() {
        when(repository.existsById("id")).thenReturn(true);
        boolean result = service.isExemptUserById("id");

        assertThat(result).isTrue();
        verify(repository).existsById("id");
    }

    @Test
    void returnsFalseWhenNoExemptUserById(){
        when(repository.existsById("id")).thenReturn(false);
        boolean result = service.isExemptUserById("id");

        assertThat(result).isFalse();
        verify(repository).existsById("id");
    }


    @Test
    void mapsDataAccessFailureToInternalServerErrorWhenGettingByEmail() {
        when(repository.findByEmail("test@example.com"))
                .thenThrow(new DataAccessResourceFailureException("mongo unavailable"));

        assertThatThrownBy(() -> service.getExemptUserByEmail("test@example.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void mapsDataIntegrityFailureToInternalServerErrorWhenGettingByEmail() {
        when(repository.findByEmail("test@example.com"))
                .thenThrow(new IncorrectResultSizeDataAccessException(1, 2));

        assertThatThrownBy(() -> service.getExemptUserByEmail("test@example.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
    @Test
    void mapsDataAccessFailureToInternalServerErrorWhenGettingById() {
        when(repository.findById("1111"))
            .thenThrow(new DataAccessResourceFailureException("mongo unavailable"));

        assertThatThrownBy(() -> service.getExemptUserById("1111"))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void mapsDataIntegrityFailureToInternalServerErrorWhenGettingById() {
        when(repository.findById("1111"))
            .thenThrow(new IncorrectResultSizeDataAccessException(1, 2));

        assertThatThrownBy(() -> service.getExemptUserById("1111"))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void mapsDataAccessFailureToInternalServerErrorWhenCheckingById() {
        when(repository.existsById("id"))
                .thenThrow(new DataAccessResourceFailureException("mongo unavailable"));

        assertThatThrownBy(() -> service.isExemptUserById("id"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}

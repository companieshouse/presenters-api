package uk.gov.companieshouse.presentersapi.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;
import uk.gov.companieshouse.presentersapi.repository.ExemptUserRepository;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExemptUserServiceImplTest {

    @Mock
    private ExemptUserRepository repository;
    @Mock
    private ExemptUserServiceImpl service;
    @Mock
    private Logger logger;

    @BeforeEach
    void setUp(){
        this.service = new ExemptUserServiceImpl(repository, logger);
    }

    @Test
    void getsExemptUserByEmail() {
        // Arrange
        ExemptUserDao mockUser = new ExemptUserDao();
        mockUser.setEmail("test@example.com");
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        // Act
        Optional<ExemptUserDao> result = service.getExemptUserByEmail("test@example.com");

        // Assert
        assertThat(result.get().getEmail().equals("test@example.com"));
        verify(repository).findByEmail("test@example.com");
    }

    @Test
    void getsExemptUserById() {
        ExemptUserDao mockUser = new ExemptUserDao();
        mockUser.setId("6ba129e2939hf");

        when(repository.findById("id")).thenReturn(Optional.of(mockUser));
        Optional<ExemptUserDao> result = service.getExemptUserById("id");

        assertThat(result.get().getId().equals("6ba129e2939hf"));
        verify(repository).findById("id");
    }


    @Test
    void isExemptUserByEmail(){
        when(repository.existsByEmail("test@example.com")).thenReturn(true);
        Boolean result = service.isExemptUserByEmail("test@example.com");

        assertThat(result).isTrue();

        verify(repository).existsByEmail("test@example.com");
    }

    @Test
    void isNotExemptUserByEmail(){
        when(repository.existsByEmail("test@example.com")).thenReturn(false);
        boolean result = service.isExemptUserByEmail("test@example.com");

        assertThat(result).isFalse();
        verify(repository).existsByEmail("test@example.com");
    }

    @Test
    void isExemptUserById(){
        when(repository.existsById("id")).thenReturn(true);
        boolean result = service.isExemptUserById("id");

        assertThat(result).isTrue();
        verify(repository).existsById("id");
    }

    @Test
    void isNotExemptUserById(){
        when(repository.existsById("id")).thenReturn(false);
        boolean result = service.isExemptUserById("id");

        assertThat(result).isFalse();
        verify(repository).existsById("id");
    }
}

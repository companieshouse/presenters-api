package uk.gov.companieshouse.presentersapi.service.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;
import uk.gov.companieshouse.presentersapi.repository.ExemptUserRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExemptUserServiceImplTest {

    @Mock
    private ExemptUserRepository repository;
    @InjectMocks
    private ExemptUserServiceImpl service;


    @BeforeEach
    void setUp(){
        this.service = new ExemptUserServiceImpl(repository, mock(Logger.class));
    }

    @Test
    void getsExemptUserByEmail() {
        // Arrange
        ExemptUserDao mockUser = new ExemptUserDao();
        mockUser.setEmail("test@example.com");
        when(repository.findByEmail("test@example.com")).thenReturn(mockUser);

        // Act
        ExemptUserDao result = service.getExemptUserByEmail("test@example.com");

        // Assert
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(repository).findByEmail("test@example.com");
    }

    @Test
    void getsExemptUserById() {
        ExemptUserDao mockUser = new ExemptUserDao();
        mockUser.setId("6ba129e2939hf");

        when(repository.findByObjectId("id")).thenReturn(mockUser);
        ExemptUserDao result = service.getExemptUserById("id");

        assertThat(result.getId()).isEqualTo("6ba129e2939hf");
        verify(repository).findByObjectId("id");
    }


    @Test
    void isExemptUserByEmail(){
        ExemptUserDao mockUser = new ExemptUserDao();
        mockUser.setEmail("test@example.com");

        when(repository.findByEmail("test@example.com")).thenReturn(mockUser);
        Boolean result = service.isExemptUserByEmail("test@example.com");

        assertThat(result).isTrue();

        verify(repository).findByEmail("test@example.com");
    }

    @Test
    void isNotExemptUserByEmail(){
        when(repository.findByEmail("test@example.com")).thenReturn(null);
        Boolean result = service.isExemptUserByEmail("test@example.com");

        assertThat(result).isFalse();
        verify(repository).findByEmail("test@example.com");
    }

    @Test
    void isExemptUserById(){
        ExemptUserDao mockUser = new ExemptUserDao();
        mockUser.setId("6ba129e2939hf");

        when(repository.findByObjectId("id")).thenReturn(mockUser);
        Boolean result = service.isExemptUserById("id");

        assertThat(result).isTrue();
        verify(repository).findByObjectId("id");
    }

    @Test
    void isNotExemptUserById(){
        when(repository.findByObjectId("id")).thenReturn(null);
        Boolean result = service.isExemptUserById("id");

        assertThat(result).isFalse();
        verify(repository).findByObjectId("id");
    }


}
